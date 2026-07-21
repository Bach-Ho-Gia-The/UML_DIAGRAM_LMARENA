package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransaction extends BaseEntity {

    @Column(name = "order_code", nullable = false, unique = true)
    Long orderCode; // Used for PayOS orderCode

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    Plan plan;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus status;

    @Column(name = "checkout_url", length = 1000)
    String checkoutUrl;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    // ─── Subscription Phase 1 (Chặng 1A, additive nullable) — snapshot bất biến của quote/upgrade ───
    /** NEW_SUBSCRIPTION | UPGRADE. Null cho các giao dịch cũ (mua phẳng trước Phase 1). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    su26.uml.be.enums.PaymentTransactionType type;

    @Column(name = "source_subscription_id")
    java.util.UUID sourceSubscriptionId;

    @Column(name = "source_plan_id")
    java.util.UUID sourcePlanId;

    @Column(name = "target_plan_id")
    java.util.UUID targetPlanId;

    @Column(name = "old_price_snapshot", precision = 18, scale = 2)
    BigDecimal oldPriceSnapshot;

    @Column(name = "new_price_snapshot", precision = 18, scale = 2)
    BigDecimal newPriceSnapshot;

    @Column(name = "price_difference", precision = 18, scale = 2)
    BigDecimal priceDifference;

    @Column(name = "billing_remaining_ratio", precision = 10, scale = 6)
    BigDecimal billingRemainingRatio;

    @Column(name = "quota_remaining_ratio", precision = 10, scale = 6)
    BigDecimal quotaRemainingRatio;

    @Column(name = "old_nominal_quota")
    Integer oldNominalQuota;

    @Column(name = "new_nominal_quota")
    Integer newNominalQuota;

    @Column(name = "quota_delta")
    Integer quotaDelta;

    @Column(name = "new_effective_limit")
    Integer newEffectiveLimit;

    @Column(name = "quote_created_at")
    LocalDateTime quoteCreatedAt;

    @Column(name = "quote_expires_at")
    LocalDateTime quoteExpiresAt;
}
