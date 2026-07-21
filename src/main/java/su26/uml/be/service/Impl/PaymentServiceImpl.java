package su26.uml.be.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import su26.uml.be.dto.payment.PaymentResponse;
import su26.uml.be.dto.payment.PaymentStatusResponse;
import su26.uml.be.entity.PaymentTransaction;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.Subscription;
import su26.uml.be.entity.User;
import su26.uml.be.enums.PaymentStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.PaymentTransactionRepository;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.PaymentService;
import su26.uml.be.service.QuotaService;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;
    private final PlanRepository planRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final QuotaService quotaService;
    private final su26.uml.be.service.SubscriptionActivationService subscriptionActivationService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendUrl;

    /** Chặng 2 cutover: ON → dùng SubscriptionActivationService idempotent; OFF → flow cũ inline. */
    @Value("${feature.entitlement-v2-enabled:false}")
    private boolean entitlementV2Enabled;

    /**
     * Loại bỏ dấu tiếng Việt và ký tự đặc biệt để tuân thủ yêu cầu ASCII của PayOS.
     * PayOS không chấp nhận description có ký tự ngoài ASCII.
     */
    private String toAsciiSafe(String input) {
        if (input == null) return "";
        // Decompose Unicode characters (e.g. ộ -> o + combining marks), then remove combining marks
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Remove all non-ASCII characters (combining diacritical marks fall in range \u0300-\u036F)
        return normalized.replaceAll("[^\\x00-\\x7F]", "").trim();
    }

    @Override
    public PaymentResponse createPaymentLink(User user, UUID planId, String returnUrl, String cancelUrl) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // Generate a unique order code for the transaction
        // PayOS requires orderCode < 9,007,199,254,740,991 (JS MAX_SAFE_INTEGER)
        // Use epoch seconds (10 digits) + 2 random digits = 12 digits max → safe
        String randomSuffix = String.format("%02d", new java.util.Random().nextInt(100));
        long epochSeconds = System.currentTimeMillis() / 1000; // 10 chữ số
        Long orderCode = Long.parseLong(epochSeconds + randomSuffix); // tối đa 12 chữ số

        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderCode(orderCode)
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        paymentTransactionRepository.save(transaction);
        return createPayosLink(transaction, returnUrl, cancelUrl);
    }

    @Override
    public PaymentResponse createPayosLink(PaymentTransaction transaction, String returnUrl, String cancelUrl) {
        Plan plan = transaction.getPlan();
        Long orderCode = transaction.getOrderCode();
        try {
            // Sử dụng returnUrl và cancelUrl truyền từ request, hoặc lấy default nếu không có
            String finalReturnUrl = (returnUrl != null && !returnUrl.trim().isEmpty())
                    ? returnUrl
                    : frontendUrl + "/";

            String finalCancelUrl = (cancelUrl != null && !cancelUrl.trim().isEmpty())
                    ? cancelUrl
                    : frontendUrl + "/";

            // PayOS chỉ chấp nhận ASCII thuần túy, tối đa 25 ký tự
            String rawDescription = "Thanh toan goi " + plan.getName();
            String safeDescription = toAsciiSafe(rawDescription);
            String description = safeDescription.length() > 25
                    ? safeDescription.substring(0, 25)
                    : safeDescription;

            // Số tiền thực thu = amount của transaction (NEW = giá gói; UPGRADE = prorated).
            long amountInVND = transaction.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();

            log.info("Creating PayOS payment: orderCode={}, amountInVND={}, description='{}'",
                    orderCode, amountInVND, description);

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amountInVND)
                    .description(description)
                    .returnUrl(finalReturnUrl)
                    .cancelUrl(finalCancelUrl)
                    .build();

            CreatePaymentLinkResponse checkoutResponse = payOS.paymentRequests().create(paymentData);

            transaction.setCheckoutUrl(checkoutResponse.getCheckoutUrl());
            paymentTransactionRepository.save(transaction);

            return PaymentResponse.builder()
                    .checkoutUrl(checkoutResponse.getCheckoutUrl())
                    .orderCode(orderCode)
                    .qrCode(checkoutResponse.getQrCode())
                    .build();

        } catch (AppException e) {
            throw e; // Re-throw typed exceptions as-is
        } catch (Exception e) {
            log.error("Error creating payment link with PayOS: orderCode={}, error={}",
                    orderCode, e.getMessage(), e);
            throw new AppException(ErrorCode.PAYMENT_LINK_CREATION_FAILED);
        }
    }

    @Override
    public void processWebhook(WebhookData webhookData) {
        try {
            Long orderCode = webhookData.getOrderCode();
            log.info("Processing webhook for orderCode: {}", orderCode);

            // Check if webhook represents a successful payment
            if (!"00".equals(webhookData.getCode())) {
                log.info("Webhook event is not a successful payment. Code: {}", webhookData.getCode());
                return;
            }

            // Chặng 2 cutover: activation idempotent tập trung (webhook + polling dùng chung).
            if (entitlementV2Enabled) {
                subscriptionActivationService.activate(orderCode);
                return;
            }

            Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository
                    .findByOrderCodeAndStatus(orderCode, PaymentStatus.PENDING);

            if (transactionOpt.isEmpty()) {
                log.warn("Transaction not found or already processed for orderCode: {}", orderCode);
                return;
            }

            PaymentTransaction transaction = transactionOpt.get();
            transaction.setStatus(PaymentStatus.PAID);
            paymentTransactionRepository.save(transaction);

            // Grant subscription to user
            User user = transaction.getUser();
            Plan plan = transaction.getPlan();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = now;
            
            // If user has an active subscription, start the new one from the end date of the current one
            Subscription currentSub = user.getCurrentSubscription();
            if (currentSub != null && currentSub.getEndDate() != null && currentSub.getEndDate().isAfter(now)) {
                startDate = currentSub.getEndDate();
                currentSub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(currentSub);
            }
            
            LocalDateTime endDate = startDate.plusDays(plan.getDurationDays() != null ? plan.getDurationDays() : 30);

            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .status(SubscriptionStatus.ACTIVE)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            subscriptionRepository.save(subscription);

            user.setCurrentSubscription(subscription);
            userRepository.save(user);

            // Reset quota theo gói mới: used=0, limit=gói mới, reset_at=now+30d (thanh quota hiện gói mới).
            quotaService.resetOnPlanChange(user.getId());

            log.info("Successfully granted plan {} to user {}", plan.getName(), user.getUsername());

        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            throw new RuntimeException("Webhook processing failed");
        }
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(Long orderCode) {
        PaymentTransaction transaction = paymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.getStatus() == PaymentStatus.PENDING) {
            try {
                vn.payos.model.v2.paymentRequests.PaymentLink linkData = payOS.paymentRequests().get(orderCode);
                if (vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PAID.equals(linkData.getStatus())) {
                    // Chặng 2 cutover: activation idempotent tập trung; reload để response phản ánh PAID.
                    if (entitlementV2Enabled) {
                        subscriptionActivationService.activate(orderCode);
                        transaction = paymentTransactionRepository.findByOrderCode(orderCode).orElse(transaction);
                        return PaymentStatusResponse.builder()
                                .orderCode(transaction.getOrderCode())
                                .status(transaction.getStatus().name())
                                .planName(transaction.getPlan().getName())
                                .build();
                    }
                    transaction.setStatus(PaymentStatus.PAID);
                    paymentTransactionRepository.save(transaction);

                    User user = transaction.getUser();
                    Plan plan = transaction.getPlan();
        
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startDate = now;
                    
                    Subscription currentSub = user.getCurrentSubscription();
                    if (currentSub != null && currentSub.getEndDate() != null && currentSub.getEndDate().isAfter(now)) {
                        startDate = currentSub.getEndDate();
                        currentSub.setStatus(SubscriptionStatus.EXPIRED);
                        subscriptionRepository.save(currentSub);
                    }
                    
                    LocalDateTime endDate = startDate.plusDays(plan.getDurationDays() != null ? plan.getDurationDays() : 30);
        
                    Subscription subscription = Subscription.builder()
                            .user(user)
                            .plan(plan)
                            .status(SubscriptionStatus.ACTIVE)
                            .startDate(startDate)
                            .endDate(endDate)
                            .build();
        
                    subscriptionRepository.save(subscription);
        
                    user.setCurrentSubscription(subscription);
                    userRepository.save(user);
        
                    quotaService.resetOnPlanChange(user.getId());
                    log.info("Successfully synced and granted plan {} to user {}", plan.getName(), user.getUsername());
                } else if (vn.payos.model.v2.paymentRequests.PaymentLinkStatus.CANCELLED.equals(linkData.getStatus())) {
                    transaction.setStatus(PaymentStatus.CANCELLED);
                    paymentTransactionRepository.save(transaction);
                }
            } catch (Exception e) {
                log.error("Failed to fetch payment status from PayOS for orderCode: " + orderCode, e);
            }
        }

        return PaymentStatusResponse.builder()
                .orderCode(transaction.getOrderCode())
                .status(transaction.getStatus().name())
                .planName(transaction.getPlan().getName())
                .build();
    }
}
