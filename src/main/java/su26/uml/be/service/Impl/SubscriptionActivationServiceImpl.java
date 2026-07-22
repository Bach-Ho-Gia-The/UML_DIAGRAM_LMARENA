package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;
import su26.uml.be.entity.Subscription;
import su26.uml.be.entity.User;
import su26.uml.be.entity.PaymentTransaction;
import su26.uml.be.enums.PaymentStatus;
import su26.uml.be.enums.PaymentTransactionType;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.PaymentTransactionRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.QuotaService;
import su26.uml.be.service.SubscriptionAccessService;
import su26.uml.be.service.SubscriptionActivationService;

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
        subscriptionRepository.save(sub);

        user.setCurrentSubscription(sub);
        userRepository.save(user);

        // Kỳ mới: reset used=0, limit=gói mới, resetAt theo endDate.
        quotaService.resetOnPlanChange(user.getId());
    }

    /** Nâng cấp giữa kỳ: thay sub cũ (REPLACED), giữ nguyên kỳ (không kéo dài), quota giữ used. */
    private void activateUpgrade(User user, PaymentTransaction tx) {
        LocalDateTime now = LocalDateTime.now();
        Plan target = tx.getPlan();

        Subscription oldSub = subscriptionAccessService.getActiveSubscription(user.getId(), now).orElse(null);
        LocalDateTime end = oldSub != null ? oldSub.getEndDate() : now.plusDays(periodDaysOf(target));
        if (oldSub != null) {
            oldSub.setStatus(SubscriptionStatus.REPLACED);
            subscriptionRepository.save(oldSub);
        }

        Subscription sub = Subscription.builder()
                .user(user)
                .plan(target)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(end) // BR-UPGRADE-06: không kéo dài kỳ
                .billingPriceSnapshot(target.getPrice()) // giá full gói mới → nền cho lần upgrade sau
                .currencySnapshot(target.getCurrency())
                .billingCycleSnapshot("MONTHLY")
                .nominalAiLimitSnapshot(aiLimitOf(target))
                .build();
        subscriptionRepository.save(sub);

        user.setCurrentSubscription(sub);
        userRepository.save(user);

        // Quota: giữ used, set effective limit theo quote (fallback nominal gói mới nếu thiếu snapshot).
        int newLimit = tx.getNewEffectiveLimit() != null ? tx.getNewEffectiveLimit() : aiLimitOf(target);
        quotaService.applyUpgrade(user.getId(), target, newLimit, sub.getId());
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
