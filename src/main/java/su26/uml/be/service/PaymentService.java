package su26.uml.be.service;

import su26.uml.be.dto.payment.PaymentResponse;
import su26.uml.be.dto.payment.PaymentStatusResponse;
import su26.uml.be.entity.PaymentTransaction;
import su26.uml.be.entity.User;
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
