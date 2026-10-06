package su26.uml.be.features.subscription.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.billing.dto.PaymentResponse;
import su26.uml.be.features.subscription.dto.UpgradeQuoteResponse;
import su26.uml.be.features.billing.entity.PaymentTransaction;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.PaymentStatus;
import su26.uml.be.common.constant.enums.PaymentTransactionType;
import su26.uml.be.common.constant.enums.UpgradeMode;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.billing.repository.PaymentTransactionRepository;
import su26.uml.be.features.subscription.repository.SubscriptionRepository;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.billing.service.PaymentService;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.subscription.service.UpgradePaymentService;
import su26.uml.be.features.subscription.service.UpgradeQuoteService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UpgradePaymentServiceImpl implements UpgradePaymentService {

    PaymentTransactionRepository paymentTransactionRepository;
    SubscriptionRepository subscriptionRepository;
    PlanRepository planRepository;
    SubscriptionAccessService subscriptionAccessService;
    UpgradeQuoteService upgradeQuoteService;
    PaymentService paymentService;

    @Override
    @Transactional
    public PaymentResponse createIntentPayment(User user, UUID targetPlanId, String returnUrl, String cancelUrl, UpgradeMode upgradeMode) {
        UUID userId = user.getId();

        if (paymentTransactionRepository.existsByUser_IdAndStatus(userId, PaymentStatus.PENDING)) {
            throw new AppException(ErrorCode.PENDING_PAYMENT_EXISTS);
        }

        Plan target = planRepository.findById(targetPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
        if (target.getTierOrder() == null) {
            throw new AppException(ErrorCode.PLAN_TIER_NOT_CONFIGURED);
        }
        // T17 (V3): gói báo giá có price = null — amount của PayOS sẽ null → chặn trước.
        if (target.isContactSales()) {
            throw new AppException(ErrorCode.PLAN_CONTACT_SALES_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        Subscription current = subscriptionAccessService.getActiveSubscription(userId, now).orElse(null);
        if (current != null && current.getPlan().isContactSales()) {
            // Đang ở gói báo giá: không thể tự nâng/hạ trực tuyến — phải liên hệ sales.
            throw new AppException(ErrorCode.PLAN_CONTACT_SALES_REQUIRED);
        }

        PaymentTransaction tx = current == null
                ? buildNewPurchase(user, target)
                : buildUpgrade(user, target, current, upgradeMode);

        paymentTransactionRepository.save(tx);
        return paymentService.createPayosLink(tx, returnUrl, cancelUrl);
    }

    private PaymentTransaction buildNewPurchase(User user, Plan target) {
        return baseTx(user, target, PaymentTransactionType.NEW_SUBSCRIPTION, target.getPrice())
                .targetPlanId(target.getId())
                .newPriceSnapshot(target.getPrice())
                .build();
    }

    private PaymentTransaction buildUpgrade(User user, Plan target, Subscription current, UpgradeMode mode) {
        Plan currentPlan = current.getPlan();
        if (Objects.equals(currentPlan.getId(), target.getId())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }
        if (currentPlan.getTierOrder() == null) {
            throw new AppException(ErrorCode.PLAN_TIER_NOT_CONFIGURED);
        }
        // T17: hạ gói KHÔNG còn bị chặn "DOWNGRADE_NOT_ALLOWED_WHILE_ACTIVE" — hướng user sang
        // endpoint hạ cấp (booked downgrade). Cùng bậc nhưng khác gói = đang dùng gói bậc đó.
        if (target.getTierOrder() < currentPlan.getTierOrder()) {
            throw new AppException(ErrorCode.USE_UPGRADE_ENDPOINT);
        }
        if (target.getTierOrder().equals(currentPlan.getTierOrder())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // T17 (C7 §C): user vừa kích hoạt nâng cấp → xoá mọi thay đổi pending (hạ cấp đã đặt)
        // trên subscription hiện tại, tránh trạng thái "vừa pending hạ, vừa lên gói mới".
        if (current.getPendingPlanId() != null || current.getPendingEffectiveAt() != null) {
            current.setPendingPlanId(null);
            current.setPendingEffectiveAt(null);
            subscriptionRepository.save(current);
        }

        UpgradeQuoteResponse q = upgradeQuoteService.getQuote(user.getEmail(), target.getId(), mode);

        return baseTx(user, target, PaymentTransactionType.UPGRADE, q.getAmountToPay())
                .upgradeMode(mode)
                .sourceSubscriptionId(current.getId())
                .sourcePlanId(currentPlan.getId())
                .targetPlanId(target.getId())
                .oldPriceSnapshot(q.getOldPrice())
                .newPriceSnapshot(q.getNewPrice())
                .priceDifference(q.getPriceDifference())
                .billingRemainingRatio(q.getBillingRemainingRatio())
                .quotaRemainingRatio(q.getQuotaRemainingRatio())
                .oldNominalQuota(q.getOldNominalQuota())
                .newNominalQuota(q.getNewNominalQuota())
                .quotaDelta(q.getQuotaDelta())
                .newEffectiveLimit(q.getNewEffectiveLimit())
                .quoteCreatedAt(LocalDateTime.now())
                .quoteExpiresAt(q.getQuoteExpiresAt())
                .build();
    }

    private PaymentTransaction.PaymentTransactionBuilder<?, ?> baseTx(
            User user, Plan plan, PaymentTransactionType type, BigDecimal amount) {
        return PaymentTransaction.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .plan(plan)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .type(type)
                .createdAt(LocalDateTime.now());
    }

    private Long generateOrderCode() {
        String randomSuffix = String.format("%02d", new java.util.Random().nextInt(100));
        long epochSeconds = System.currentTimeMillis() / 1000;
        return Long.parseLong(epochSeconds + randomSuffix);
    }
}