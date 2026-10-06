package su26.uml.be.features.subscription.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.plan.entity.PlanFeature;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.billing.entity.PaymentTransaction;
import su26.uml.be.common.constant.enums.PaymentStatus;
import su26.uml.be.common.constant.enums.PaymentTransactionType;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.constant.enums.SubscriptionStatus;
import su26.uml.be.common.constant.enums.UpgradeMode;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.billing.repository.PaymentTransactionRepository;
import su26.uml.be.features.subscription.repository.SubscriptionRepository;
import su26.uml.be.features.user.repository.UserRepository;
import su26.uml.be.features.usage.service.QuotaService;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.subscription.service.SubscriptionActivationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubscriptionActivationServiceImpl implements SubscriptionActivationService {

    PaymentTransactionRepository paymentTransactionRepository;
    SubscriptionRepository subscriptionRepository;
    UserRepository userRepository;
    SubscriptionAccessService subscriptionAccessService;
    QuotaService quotaService;

    @Override
    @Transactional
    public void activate(Long orderCode) {
        // Chốt idempotent: chỉ 1 luồng chuyển được PENDING→PAID → chỉ 1 luồng cấp quyền (R3).
        int changed = paymentTransactionRepository.markPaidIfPending(
                orderCode, PaymentStatus.PAID, PaymentStatus.PENDING);
        if (changed == 0) {
            log.info("activate skip: orderCode={} không ở PENDING (đã xử lý hoặc không tồn tại)", orderCode);
            return;
        }

        PaymentTransaction tx = paymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        User user = tx.getUser();

        if (tx.getType() == PaymentTransactionType.UPGRADE) {
            activateUpgrade(user, tx);
        } else {
            activateNew(user, tx);
        }

        // T22: kích hoạt thành công → xoá mọi thay đổi pending (hạ cấp đã đặt) của user.
        // Không clear thì banner "sắp hạ gói" vẫn hiện dù user vừa mua/nâng gói mới.
        int cleared = subscriptionRepository.clearPendingForUser(user.getId());
        if (cleared > 0) {
            log.info("Cleared {} pending plan change(s) for user {} after activation (orderCode={})",
                    cleared, user.getUsername(), orderCode);
        }

        log.info("Activated {} for user {} (orderCode={})",
                tx.getType(), user.getUsername(), orderCode);
    }

    /** Mua gói mới: chỉ khi không có paid sub effective (đã đảm bảo lúc tạo payment). */
    private void activateNew(User user, PaymentTransaction tx) {
        Plan plan = tx.getPlan();
        LocalDateTime now = LocalDateTime.now();
        Subscription sub = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(now.plusDays(periodDaysOf(plan)))
                .billingPriceSnapshot(tx.getAmount())
                .currencySnapshot(plan.getCurrency())
                .billingCycleSnapshot("MONTHLY")
                .nominalAiLimitSnapshot(aiLimitOf(plan))
                .build();
        applyEntitlementSnapshot(sub, plan); // đóng băng quyền lợi lúc mua
        subscriptionRepository.save(sub);

        user.setCurrentSubscription(sub);
        userRepository.save(user);

        // Kỳ mới: reset used=0, limit=gói mới, resetAt theo endDate.
        quotaService.resetOnPlanChange(user.getId());
    }

    /** Nâng cấp giữa kỳ: thay sub cũ (REPLACED). Tuỳ upgradeMode mà reset toàn bộ hay giữ used. */
    private void activateUpgrade(User user, PaymentTransaction tx) {
        LocalDateTime now = LocalDateTime.now();
        Plan target = tx.getPlan();
        UpgradeMode mode = tx.getUpgradeMode();

        Subscription oldSub = subscriptionAccessService.getActiveSubscription(user.getId(), now).orElse(null);
        if (oldSub != null) {
            oldSub.setStatus(SubscriptionStatus.REPLACED);
            subscriptionRepository.save(oldSub);
        }

        // DIRECT: kỳ mới (endDate = now + periodDays). PRORATED/NULL: giữ nguyên kỳ cũ.
        LocalDateTime end;
        if (mode == UpgradeMode.DIRECT) {
            end = now.plusDays(periodDaysOf(target));
        } else {
            end = oldSub != null ? oldSub.getEndDate() : now.plusDays(periodDaysOf(target));
        }

        // Defense-in-depth: cả 2 đường tạo payment (PaymentServiceImpl.createPaymentLink và
        // UpgradePaymentServiceImpl.createIntentPayment) đã chặn gói contactSales trước khi vào đây,
        // nên target.getPrice() khác null. Guard ZERO để snapshot không bao giờ null.
        Subscription sub = Subscription.builder()
                .user(user)
                .plan(target)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(end)
                .billingPriceSnapshot(target.getPrice() != null ? target.getPrice() : java.math.BigDecimal.ZERO)
                .currencySnapshot(target.getCurrency())
                .billingCycleSnapshot("MONTHLY")
                .nominalAiLimitSnapshot(aiLimitOf(target))
                .build();
        applyEntitlementSnapshot(sub, target); // đóng băng quyền lợi gói mới
        subscriptionRepository.save(sub);

        user.setCurrentSubscription(sub);
        userRepository.save(user);

        if (mode == UpgradeMode.DIRECT) {
            // Reset toàn bộ quota như mua mới (used = 0, limit = full gói mới, kỳ quota mới)
            quotaService.resetOnPlanChange(user.getId());
        } else {
            // PRORATED: giữ used, set effective limit theo quote
            int newLimit = tx.getNewEffectiveLimit() != null ? tx.getNewEffectiveLimit() : aiLimitOf(target);
            quotaService.applyUpgrade(user.getId(), target, newLimit, sub.getId());
        }
    }

    private int periodDaysOf(Plan plan) {
        if (plan.getQuotaPeriodDays() != null && plan.getQuotaPeriodDays() > 0) {
            return plan.getQuotaPeriodDays();
        }
        return plan.getDurationDays() != null && plan.getDurationDays() > 0 ? plan.getDurationDays() : 30;
    }

    private int aiLimitOf(Plan plan) {
        return limitOf(plan, PlanFeatureKey.AI_QUERIES);
    }

    private int limitOf(Plan plan, PlanFeatureKey key) {
        if (plan == null) {
            return 0;
        }
        return plan.getPlanFeatures().stream()
                .filter(f -> f.getFeatureKey() == key)
                .map(PlanFeature::getLimitValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(0);
    }

    /**
     * Đóng băng quyền lợi của gói vào subscription (snapshot). Sau bước này, admin sửa metadata
     * gói (kể cả khi gói đang ACTIVE/ARCHIVED) không làm thay đổi quota/limits/rate-limit mà
     * user đang được hưởng cho tới hết kỳ. Renew sau hết kỳ sẽ snapshot lại theo metadata mới.
     */
    private void applyEntitlementSnapshot(Subscription sub, Plan plan) {
        if (plan == null) {
            return;
        }
        sub.setSnapshotPlanName(plan.getName());
        sub.setSnapshotPlanDescription(plan.getDescription());
        sub.setSnapshotAiQueries(limitOf(plan, PlanFeatureKey.AI_QUERIES));
        sub.setSnapshotMaxProjects(limitOf(plan, PlanFeatureKey.MAX_PROJECTS));
        sub.setSnapshotMaxDiagrams(limitOf(plan, PlanFeatureKey.MAX_DIAGRAMS));
        sub.setSnapshotMaxExportPdf(limitOf(plan, PlanFeatureKey.EXPORT_PDF));
        sub.setSnapshotMaxCollaborators(limitOf(plan, PlanFeatureKey.MAX_COLLABORATORS));
        sub.setSnapshotRateLimitPer10s(plan.getRateLimitPer10s());
        sub.setSnapshotRateLimitPerMin(plan.getRateLimitPerMin());
    }
}