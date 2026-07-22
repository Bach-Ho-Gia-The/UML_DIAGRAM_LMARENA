package su26.uml.be.features.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Nhật ký thao tác quản trị (append-only). Chỉ INSERT — không bao giờ UPDATE/DELETE.
 *
 * <p>Không kế thừa {@code BaseEntity} vì audit log dùng khóa số tự tăng (thứ tự chèn tự nhiên)
 * và mốc thời gian {@link Instant} (UTC) thay cho {@code LocalDateTime}.</p>
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** UUID của admin thực hiện (null nếu không xác định được từ SecurityContext). */
    @Column(name = "actor_id")
    UUID actorId;

    @Column(name = "actor_email")
    String actorEmail;

    @Column(name = "actor_name")
    String actorName;

    @Column(name = "action", nullable = false, length = 100)
    String action;

    @Column(name = "target_type", length = 100)
    String targetType;

    @Column(name = "target_id")
    String targetId;

    /** Chi tiết dạng JSON (vd. {"before":"ACTIVE","after":"LOCKED"}). Lưu text để portable. */
    @Column(name = "detail", columnDefinition = "text")
    String detail;

    @Column(name = "ip_address", length = 64)
    String ipAddress;

    /** Mốc thời gian UTC (Instant). @JsonFormat UTC được đặt ở tầng response DTO. */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    Instant createdAt;
}