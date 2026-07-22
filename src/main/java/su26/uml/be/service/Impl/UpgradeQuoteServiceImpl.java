package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.dto.subscription.PlanSnapshot;
import su26.uml.be.dto.subscription.QuotePairResponse;
import su26.uml.be.dto.subscription.QuotaSnapshot;
import su26.uml.be.dto.subscription.SubscriptionSnapshot;
import su26.uml.be.dto.subscription.UpgradeQuoteResponse;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.UpgradeMode;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.QuotaPeriodService;
import su26.uml.be.service.SubscriptionAccessService;
import su26.uml.be.service.UpgradeCalculator;
import su26.uml.be.service.UpgradeQuoteService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UpgradeQuoteServiceImpl implements UpgradeQuoteService {

    UserRepository userRepository;
    PlanRepository planRepository;
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

        Subscription current = requireActiveSubscription(userId, now);
        Plan targetPlan = requirePlan(targetPlanId);

        SubscriptionSnapshot currentSnap = buildSubscriptionSnapshot(current);
        PlanSnapshot targetSnap = buildPlanSnapshot(targetPlan);
        QuotaSnapshot quotaSnap = quotaPeriodService.getCurrentSnapshot(userId);

        UpgradeQuoteResponse quote = upgradeCalculator.calculate(currentSnap, targetSnap, quotaSnap, now);
        quote.setQuoteExpiresAt(now.plusMinutes(quoteTtlMinutes));
        return quote;
    }

    private UpgradeQuoteResponse getDirectQuote(String email, UUID targetPlanId) {
        UUID userId = resolveUserId(email);
        LocalDateTime now = LocalDateTime.now();

        Subscription current = requireActiveSubscription(userId, now);
        Plan targetPlan = requirePlan(targetPlanId);

        SubscriptionSnapshot currentSnap = buildSubscriptionSnapshot(current);
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

    private Subscription requireActiveSubscription(UUID userId, LocalDateTime now) {
        return subscriptionAccessService.getActiveSubscription(userId, now)
                .orElseThrow(() -> new AppException(ErrorCode.UPGRADE_REQUIRES_ACTIVE_SUBSCRIPTION));
    }

    private Plan requirePlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
    }

    private SubscriptionSnapshot buildSubscriptionSnapshot(Subscription current) {
        Plan currentPlan = current.getPlan();
        return SubscriptionSnapshot.builder()
                .tierOrder(currentPlan.getTierOrder())
                .price(current.getBillingPriceSnapshot() != null ? current.getBillingPriceSnapshot() : currentPlan.getPrice())
                .currency(current.getCurrencySnapshot() != null ? current.getCurrencySnapshot() : currentPlan.getCurrency())
                .nominalAiLimit(current.getNominalAiLimitSnapshot() != null
                        ? current.getNominalAiLimitSnapshot() : aiLimitOf(currentPlan))
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
