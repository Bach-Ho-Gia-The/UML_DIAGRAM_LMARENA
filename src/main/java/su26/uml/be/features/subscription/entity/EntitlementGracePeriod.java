package su26.uml.be.features.subscription.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;
import su26.uml.be.features.user.entity.User;

/**
 * Kỳ ân hạn reconciliation (Chặng 4): khi dữ liệu ACTIVE của user vượt limit gói mới
 * (do hạ gói / hết hạn), user có 7 ngày để chọn dữ liệu giữ lại. Snapshot count/limit tại thời điểm
 * bắt đầu để hiển thị & kiểm tra selection; các mốc warning gửi email/banner ngày 0/3/6/7.
 *
 * <p>Entity hoàn toàn mới, additive — chưa có code nào ghi/đọc cho tới Chặng 4A. {@code id}/
 * {@code createdAt}/{@code updatedAt} kế thừa từ {@link BaseEntity}.
 */
@Entity
@Table(name = "entitlement_grace_periods")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EntitlementGracePeriod extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "source_plan_id")
    UUID sourcePlanId;

    @Column(name = "target_plan_id")
    UUID targetPlanId;

    /** ACTIVE / RESOLVED / ARCHIVED (lưu String để additive, chưa cần enum tới Chặng 4A). */
    @Column(name = "status", length = 20)
    String status;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "ends_at")
    LocalDateTime endsAt;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @Column(name = "archived_at")
    LocalDateTime archivedAt;

    @Column(name = "warning_day0_sent_at")
    LocalDateTime warningDay0SentAt;

    @Column(name = "warning_day3_sent_at")
    LocalDateTime warningDay3SentAt;

    @Column(name = "warning_day6_sent_at")
    LocalDateTime warningDay6SentAt;

    @Column(name = "warning_day7_sent_at")
    LocalDateTime warningDay7SentAt;

    @Column(name = "project_count_snapshot")
    Integer projectCountSnapshot;

    @Column(name = "diagram_count_snapshot")
    Integer diagramCountSnapshot;

    @Column(name = "project_limit_snapshot")
    Integer projectLimitSnapshot;

    @Column(name = "diagram_limit_snapshot")
    Integer diagramLimitSnapshot;

    @Column(name = "selection_confirmed_at")
    LocalDateTime selectionConfirmedAt;
}