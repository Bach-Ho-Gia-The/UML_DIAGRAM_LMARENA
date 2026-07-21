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
import su26.uml.be.dto.subscription.QuotaSnapshot;
import su26.uml.be.dto.subscription.SubscriptionSnapshot;
import su26.uml.be.dto.subscription.UpgradeQuoteResponse;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.PlanFeatureKey;
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
    public UpgradeQuoteResponse getQuote(String email, UUID targetPlanId) {
        UUID userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
                .getId();

        LocalDateTime now = LocalDateTime.now();

        // Phải có gói paid đang hiệu lực mới nâng cấp được (không có → mua mới, không phải upgrade).
        Subscription current = subscriptionAccessService.getActiveSubscription(userId, now)
                .orElseThrow(() -> new AppException(ErrorCode.UPGRADE_REQUIRES_ACTIVE_SUBSCRIPTION));

        Plan targetPlan = planRepository.findById(targetPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        Plan currentPlan = current.getPlan();
        SubscriptionSnapshot currentSnap = SubscriptionSnapshot.builder()
                .tierOrder(currentPlan.getTierOrder())
                // Ưu tiên snapshot đã lưu lúc mua; fallback plan live (giao dịch cũ chưa có snapshot).
                .price(current.getBillingPriceSnapshot() != null ? current.getBillingPriceSnapshot() : currentPlan.getPrice())
                .currency(current.getCurrencySnapshot() != null ? current.getCurrencySnapshot() : currentPlan.getCurrency())
                .nominalAiLimit(current.getNominalAiLimitSnapshot() != null
                        ? current.getNominalAiLimitSnapshot() : aiLimitOf(currentPlan))
                .periodStart(current.getStartDate())
                .periodEnd(current.getEndDate())
                .build();

        PlanSnapshot targetSnap = PlanSnapshot.builder()
                .planId(targetPlan.getId())
                .tierOrder(targetPlan.getTierOrder())
                .price(targetPlan.getPrice())
                .currency(targetPlan.getCurrency())
                .nominalAiLimit(aiLimitOf(targetPlan))
                .build();

        QuotaSnapshot quotaSnap = quotaPeriodService.getCurrentSnapshot(userId);

        UpgradeQuoteResponse quote = upgradeCalculator.calculate(currentSnap, targetSnap, quotaSnap, now);
        quote.setQuoteExpiresAt(now.plusMinutes(quoteTtlMinutes));
        return quote;
    }

    /** AI_QUERIES của gói; null/không có → 0. -1 = unlimited (giữ nguyên semantics QuotaServiceImpl). */
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
