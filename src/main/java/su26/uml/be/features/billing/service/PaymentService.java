package su26.uml.be.features.billing.service;

import su26.uml.be.features.billing.dto.PaymentResponse;
import su26.uml.be.features.billing.dto.PaymentStatusResponse;
import su26.uml.be.features.billing.entity.PaymentTransaction;
import su26.uml.be.features.user.entity.User;
import vn.payos.model.webhooks.WebhookData;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPaymentLink(User user, UUID planId, String returnUrl, String cancelUrl);
    void processWebhook(WebhookData webhookData);
    PaymentStatusResponse getPaymentStatus(Long orderCode);

    /**
     * Tạo link PayOS cho một transaction ĐÃ dựng + lưu (dùng chung cho mua mới & nâng cấp).
     * Số tiền lấy từ {@code transaction.amount} (NEW = giá gói, UPGRADE = prorated).
     */
    PaymentResponse createPayosLink(PaymentTransaction transaction, String returnUrl, String cancelUrl);
}