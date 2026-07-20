package su26.uml.be.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.dto.payment.PaymentRequest;
import su26.uml.be.dto.payment.PaymentResponse;
import su26.uml.be.dto.payment.PaymentStatusResponse;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.entity.User;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.PaymentService;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PayOS payOS;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PaymentRequest request) {
        if (userDetails == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        PaymentResponse response = paymentService.createPaymentLink(user, request.getPlanId(), request.getReturnUrl(), request.getCancelUrl());
        return ResponseEntity.ok(ApiResponse.success("Tạo link thanh toán thành công", response));
    }

    @GetMapping("/status/{orderCode}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable Long orderCode) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(orderCode);
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thanh toán thành công", response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Object webhookBody) {
        try {
            // Verify checksum signature using PayOS SDK v2
            WebhookData webhookData = payOS.webhooks().verify(webhookBody);
            paymentService.processWebhook(webhookData);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Webhook verification failed", e);
            return ResponseEntity.badRequest().body("Invalid webhook");
        }
    }
}
