package su26.uml.be.features.subscription.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.subscription.dto.PlanSnapshot;
import su26.uml.be.features.subscription.dto.QuotePairResponse;
import su26.uml.be.features.subscription.dto.QuotaSnapshot;
import su26.uml.be.features.subscription.dto.SubscriptionSnapshot;
import su26.uml.be.features.subscription.dto.UpgradeQuoteResponse;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.features.plan.entity.PlanFeature;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.constant.enums.UpgradeMode;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.plan.service.PlanResolutionService;
import su26.uml.be.features.user.repository.UserRepository;
import su26.uml.be.features.usage.service.QuotaPeriodService;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.subscription.service.UpgradeCalculator;
import su26.uml.be.features.subscription.service.UpgradeQuoteService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UpgradeQuoteServiceImpl implements UpgradeQuoteService {

    UserRepository userRepository;
    PlanRepository planRepository;
    PlanResolutionService planResolutionService;
    SubscriptionAccessService subscriptionAccessService;
    QuotaPeriodService quotaPeriodService;
    UpgradeCalculator upgradeCalculator;

    @Value("${subscription.quote-ttl-minutes:15}")
    @NonFinal
    int quoteTtlMinutes;

    @Override
    @Transactional(readOnly = true)
    public UpgradeQuoteResponse getQuote(String email, UUID targetPlanId, UpgradeMode mode) {
        return switch (mode) {
            case DIRECT -> getDirectQuote(email, targetPlanId);
            case PRORATED -> getProratedQuote(email, targetPlanId);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePairResponse getQuotePair(String email, UUID targetPlanId) {
        return QuotePairResponse.builder()
                .prorated(getProratedQuote(email, targetPlanId))
                .direct(getDirectQuote(email, targetPlanId))
                .build();
    }

    private UpgradeQuoteResponse getProratedQuote(String email, UUID targetPlanId) {
        UUID userId = resolveUserId(email);
        LocalDateTime now = LocalDateTime.now();

        Plan targetPlan = requirePlan(targetPlanId);

        SubscriptionSnapshot currentSnap = buildCurrentSnapshot(userId, now);
        PlanSnapshot targetSnap = buildPlanSnapshot(targetPlan);
        QuotaSnapshot quotaSnap = quotaPeriodService.getCurrentSnapshot(userId);

        UpgradeQuoteResponse quote = upgradeCalculator.calculate(currentSnap, targetSnap, quotaSnap, now);
        quote.setQuoteExpiresAt(now.plusMinutes(quoteTtlMinutes));
        return quote;
    }

    private UpgradeQuoteResponse getDirectQuote(String email, UUID targetPlanId) {
        UUID userId = resolveUserId(email);
        LocalDateTime now = LocalDateTime.now();

        Plan targetPlan = requirePlan(targetPlanId);

        SubscriptionSnapshot currentSnap = buildCurrentSnapshot(userId, now);
        PlanSnapshot targetSnap = buildPlanSnapshot(targetPlan);

        int periodDays = periodDaysOf(targetPlan);
        UpgradeQuoteResponse quote = upgradeCalculator.calculateDirect(currentSnap, targetSnap, periodDays);
        quote.setQuoteExpiresAt(now.plusMinutes(quoteTtlMinutes));
        return quote;
    }

    private UUID resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
                .getId();
    }

    /**
     * T18: snapshot gói hiện lực — user CHƯA có subscription vẫn lấy được báo giá nâng cấp
     * (dùng gói mặc định isDefaultPlan làm "current"). Trước đây ném UPGRADE_REQUIRES_ACTIVE_SUBSCRIPTION
     * nên user free không xem/đấu nối được flow nâng gấp.
     */
    private SubscriptionSnapshot buildCurrentSnapshot(UUID userId, LocalDateTime now) {
        return subscriptionAccessService.getActiveSubscription(userId, now)
                .map(this::buildSubscriptionSnapshot)
                .orElseGet(() -> {
                    Plan defaultPlan = planResolutionService.requireDefaultPlan();
                    int periodDays = periodDaysOf(defaultPlan);
                    return SubscriptionSnapshot.builder()
                            .tierOrder(defaultPlan.getTierOrder())
                            .price(defaultPlan.getPrice())
                            .currency(defaultPlan.getCurrency())
                            .nominalAiLimit(aiLimitOf(defaultPlan))
                            .contactSales(defaultPlan.isContactSales())
                            .periodStart(now)
                            .periodEnd(now.plusDays(periodDays))
                            .billingTotalDays(periodDays)
                            .build();
                });
    }

    private Plan requirePlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            // Không cho quote/nâng/hạ lên gói DRAFT/ARCHIVED.
            throw new AppException(ErrorCode.PLAN_NOT_ACTIVE);
        }
        return plan;
    }

    private SubscriptionSnapshot buildSubscriptionSnapshot(Subscription current) {
        Plan currentPlan = current.getPlan();
        return SubscriptionSnapshot.builder()
                .tierOrder(currentPlan.getTierOrder())
                .price(current.getBillingPriceSnapshot() != null ? current.getBillingPriceSnapshot() : currentPlan.getPrice())
                .currency(current.getCurrencySnapshot() != null ? current.getCurrencySnapshot() : currentPlan.getCurrency())
                .nominalAiLimit(current.getNominalAiLimitSnapshot() != null
                        ? current.getNominalAiLimitSnapshot() : aiLimitOf(currentPlan))
                .contactSales(currentPlan.isContactSales())
                .periodStart(current.getStartDate())
                .periodEnd(current.getEndDate())
                .billingTotalDays(periodDaysOf(currentPlan))
                .build();
    }

    private PlanSnapshot buildPlanSnapshot(Plan targetPlan) {
        return PlanSnapshot.builder()
                .planId(targetPlan.getId())
                .tierOrder(targetPlan.getTierOrder())
                .price(targetPlan.getPrice())
                .currency(targetPlan.getCurrency())
                .nominalAiLimit(aiLimitOf(targetPlan))
                .contactSales(targetPlan.isContactSales())
                .build();
    }

    private int periodDaysOf(Plan plan) {
        if (plan.getQuotaPeriodDays() != null && plan.getQuotaPeriodDays() > 0) {
            return plan.getQuotaPeriodDays();
        }
        return plan.getDurationDays() != null && plan.getDurationDays() > 0 ? plan.getDurationDays() : 30;
    }

    private int aiLimitOf(Plan plan) {
        if (plan == null) {
            return 0;
        }
        return plan.getPlanFeatures().stream()
                .filter(f -> f.getFeatureKey() == PlanFeatureKey.AI_QUERIES)
                .map(PlanFeature::getLimitValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(0);
    }
}