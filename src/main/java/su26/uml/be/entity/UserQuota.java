package su26.uml.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hạn mức AI Request theo kỳ của 1 user (1 dòng / user). Ví quota duy nhất.
 * Reset mỗi 30 ngày (lazy) tính từ ngày mua/đổi gói.
 */
@Entity
@Table(name = "user_quota", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserQuota extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    UUID userId;

    /** Số AI request đã dùng trong kỳ. */
    @Column(name = "ai_used", nullable = false)
    @Builder.Default
    int aiUsed = 0;

    /** Snapshot AI_QUERIES của gói lúc đầu kỳ; -1 = unlimited (bỏ qua check). */
    @Column(name = "ai_limit", nullable = false)
    @Builder.Default
    int aiLimit = 0;

    /** Số lần Export PDF đã dùng trong kỳ. */
    @Column(name = "export_used", nullable = false)
    @Builder.Default
    int exportUsed = 0;

    /** Subscription ID lúc snapshot (dùng để phát hiện đổi gói). */
    @Column(name = "subscription_id")
    UUID subscriptionId;

    /** Thời điểm reset kỳ (lazy). */
    @Column(name = "reset_at", nullable = false)
    LocalDateTime resetAt;

    // ─── Subscription Phase 1 (Chặng 1A, additive nullable) ───
    // Song song aiLimit/resetAt cũ (xem planing.md R2 — chốt cơ chế period ở Chặng 2B/3.3 trước khi dùng).
    /** Full quota của gói cho 1 kỳ đầy đủ (snapshot). */
    @Column(name = "nominal_ai_limit")
    Integer nominalAiLimit;

    /** Limit thực tế kỳ hiện tại sau proration (nominal + delta khi upgrade giữa kỳ). */
    @Column(name = "effective_ai_limit")
    Integer effectiveAiLimit;

    @Column(name = "quota_period_start")
    LocalDateTime quotaPeriodStart;

    @Column(name = "quota_period_end")
    LocalDateTime quotaPeriodEnd;

    /** Plan đang áp cho quota này (base plan khi subscriptionId = null). */
    @Column(name = "plan_id")
    UUID planId;
}
