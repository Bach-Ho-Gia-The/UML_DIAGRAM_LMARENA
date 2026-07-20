package su26.uml.be.service;

import su26.uml.be.dto.payment.PaymentResponse;
import su26.uml.be.dto.payment.PaymentStatusResponse;
import su26.uml.be.entity.User;
import vn.payos.model.webhooks.WebhookData;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPaymentLink(User user, UUID planId, String returnUrl, String cancelUrl);
    void processWebhook(WebhookData webhookData);
    PaymentStatusResponse getPaymentStatus(Long orderCode);
}
