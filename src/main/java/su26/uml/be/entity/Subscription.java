package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.enums.SubscriptionStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Column(name = "end_date")
    LocalDateTime endDate;

    // ─── Subscription Phase 1 (Chặng 1A, additive nullable) — snapshot quyền lợi lúc mua ───
    /** Giá đã trả cho kỳ này (snapshot, không đọc live catalog) — dùng cho proration & MRR. */
    @Column(name = "billing_price_snapshot", precision = 18, scale = 2)
    java.math.BigDecimal billingPriceSnapshot;

    @Column(name = "currency_snapshot", length = 10)
    String currencySnapshot;

    @Column(name = "billing_cycle_snapshot", length = 20)
    String billingCycleSnapshot;

    /** Nominal AI quota của gói lúc mua (snapshot) — nền cho tính delta khi upgrade. */
    @Column(name = "nominal_ai_limit_snapshot")
    Integer nominalAiLimitSnapshot;

    // ─── Cancel Subscription (graceful downgrade) ───
    /** True = user đã hủy gia hạn; vẫn giữ Premium tới endDate, hết kỳ về base. Status GIỮ ACTIVE. */
    @Column(name = "cancel_at_period_end", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    Boolean cancelAtPeriodEnd = false;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;
}
