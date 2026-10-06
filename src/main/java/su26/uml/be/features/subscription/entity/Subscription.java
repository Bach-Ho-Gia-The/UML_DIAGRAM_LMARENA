package su26.uml.be.features.subscription.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.common.constant.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.UUID;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.plan.entity.Plan;

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

    // ─── Entitlement snapshot (đóng băng TOÀN BỘ quyền lợi lúc mua) ───
    // Luật: admin sửa gói (kể cả gói đang ACTIVE hoặc ARCHIVED) thì user đang dùng gói đó
    // VẪN giữ quyền lợi bản cũ tới hết kỳ. Hết kỳ renew → snapshot mới theo metadata mới.
    // Nullable: sub cũ (trước khi có cơ chế này) → null → các service fallback về live plan.

    /** Tên gói tại thời điểm mua — hiển thị cho user, không đổi khi admin đổi tên gói. */
    @Column(name = "snapshot_plan_name", length = 255)
    String snapshotPlanName;

    @Column(name = "snapshot_plan_description", columnDefinition = "TEXT")
    String snapshotPlanDescription;

    /** AI quota (plan_features.AI_QUERIES). -1 = unlimited. */
    @Column(name = "snapshot_ai_queries")
    Integer snapshotAiQueries;

    @Column(name = "snapshot_max_projects")
    Integer snapshotMaxProjects;

    @Column(name = "snapshot_max_diagrams")
    Integer snapshotMaxDiagrams;

    @Column(name = "snapshot_max_export_pdf")
    Integer snapshotMaxExportPdf;

    @Column(name = "snapshot_max_collaborators")
    Integer snapshotMaxCollaborators;

    @Column(name = "snapshot_rate_limit_per10s")
    Integer snapshotRateLimitPer10s;

    @Column(name = "snapshot_rate_limit_per_min")
    Integer snapshotRateLimitPerMin;

    // ─── Cancel Subscription (graceful downgrade) ───
    /** True = user đã hủy gia hạn; vẫn giữ Premium tới endDate, hết kỳ về base. Status GIỮ ACTIVE. */
    @Column(name = "cancel_at_period_end", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    Boolean cancelAtPeriodEnd = false;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    // ─── Booked downgrade (hướng A) ───
    /**
     * Gói sẽ tự động chuyển sang khi hết kỳ hiện tại (hạ cấp đã đặt). Không thu tiền ngay —
     * user kích hoạt lại bằng POST /subscriptions/payment với pendingPlanId.
     */
    @Column(name = "pending_plan_id")
    UUID pendingPlanId;

    /** Thời điểm pendingPlanId có hiệu lực = endDate của kỳ đang chạy. */
    @Column(name = "pending_effective_at")
    LocalDateTime pendingEffectiveAt;
}