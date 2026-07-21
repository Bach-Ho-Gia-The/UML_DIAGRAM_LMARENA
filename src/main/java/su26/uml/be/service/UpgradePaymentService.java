package su26.uml.be.service;

import su26.uml.be.dto.payment.PaymentResponse;
import su26.uml.be.entity.User;

import java.util.UUID;

/**
 * Tạo payment theo intent (Chặng 2A): tự phân loại NEW_SUBSCRIPTION vs UPGRADE, chặn same-plan/
 * downgrade, chặn giao dịch PENDING trùng, snapshot quote bất biến vào PaymentTransaction.
 * Chỉ được gọi khi {@code feature.entitlement-v2-enabled = true}.
 */
public interface UpgradePaymentService {

    /** Tạo link thanh toán cho gói đích (BE tự quyết mua mới hay nâng cấp + số tiền). */
    PaymentResponse createIntentPayment(User user, UUID targetPlanId, String returnUrl, String cancelUrl);
}
