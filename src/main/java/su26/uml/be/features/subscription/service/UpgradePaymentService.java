package su26.uml.be.features.subscription.service;

import su26.uml.be.features.billing.dto.PaymentResponse;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.UpgradeMode;

import java.util.UUID;

/**
 * Tạo payment theo intent (Chặng 2A): tự phân loại NEW_SUBSCRIPTION vs UPGRADE, chặn same-plan/
 * downgrade, chặn giao dịch PENDING trùng, snapshot quote bất biến vào PaymentTransaction.
 */
public interface UpgradePaymentService {

    /** Tạo link thanh toán cho gói đích (BE tự quyết mua mới hay nâng cấp + số tiền). */
    PaymentResponse createIntentPayment(User user, UUID targetPlanId, String returnUrl, String cancelUrl, UpgradeMode upgradeMode);
}