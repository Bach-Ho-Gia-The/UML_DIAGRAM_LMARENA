package su26.uml.be.common.constant.enums;

public enum SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    /** Sub cũ bị thay thế khi user upgrade lên tier cao hơn giữa kỳ (Chặng 2). */
    REPLACED
}
