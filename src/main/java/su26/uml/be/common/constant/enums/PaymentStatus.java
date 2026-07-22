package su26.uml.be.common.constant.enums;

public enum PaymentStatus {
    PENDING,
    PAID,
    CANCELLED,
    /** Payment PAID nhưng nguồn entitlement đổi giữa chừng → cần review thủ công, KHÔNG cấp quyền lần 2 (Chặng 2). */
    REQUIRES_REVIEW
}
