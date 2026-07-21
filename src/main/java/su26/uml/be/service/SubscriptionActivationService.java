package su26.uml.be.service;

/**
 * Cấp/nâng gói từ một payment đã thanh toán — điểm activation TẬP TRUNG, idempotent
 * (thiết kế §2.7, planing.md R3). Webhook + polling đều gọi cùng service này; nhờ chốt
 * PENDING→PAID atomic, một payment chỉ được cấp quyền đúng một lần.
 */
public interface SubscriptionActivationService {

    /**
     * Idempotent: nếu payment đang PENDING → đánh dấu PAID và cấp/nâng gói theo {@code type};
     * nếu đã xử lý → bỏ qua. An toàn gọi nhiều lần / song song.
     */
    void activate(Long orderCode);
}
