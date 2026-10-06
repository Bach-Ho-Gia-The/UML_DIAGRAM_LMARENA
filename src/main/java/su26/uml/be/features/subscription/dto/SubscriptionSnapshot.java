package su26.uml.be.features.subscription.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot bất biến của gói paid hiện tại — input cho {@link su26.uml.be.service.UpgradeCalculator}.
 * Lấy từ Subscription (ưu tiên field snapshot đã lưu, fallback plan live). KHÔNG chứa entity JPA.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionSnapshot {
    Integer tierOrder;
    BigDecimal price;
    String currency;
    int nominalAiLimit;
    /** Gói báo giá (price = null) — không cho nâng/hạ tự thanh toán. */
    Boolean contactSales;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
    /** Số ngày của 1 kỳ thanh toán đầy đủ (lấy từ plan, VD: 30). KHÔNG thay đổi sau upgrade prorated. */
    int billingTotalDays;
}