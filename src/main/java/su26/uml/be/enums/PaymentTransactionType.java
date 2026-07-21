package su26.uml.be.enums;

/**
 * Phân loại giao dịch thanh toán gói.
 * <ul>
 *   <li>{@code NEW_SUBSCRIPTION} — mua gói mới khi không có paid subscription effective.</li>
 *   <li>{@code UPGRADE} — nâng lên tier cao hơn giữa kỳ (trả prorated).</li>
 * </ul>
 * Dùng bởi {@code PaymentTransaction.type} (Chặng 1A/2A).
 */
public enum PaymentTransactionType {
    NEW_SUBSCRIPTION,
    UPGRADE
}
