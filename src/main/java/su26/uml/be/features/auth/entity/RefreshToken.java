package su26.uml.be.features.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import su26.uml.be.features.user.entity.User;

/**
 * Refresh token dạng opaque, lưu server-side.
 *
 * <p>Token gửi cho client là một chuỗi ngẫu nhiên (không phải JWT, không chứa
 * username/PII). Server chỉ lưu {@code SHA-256} của token nên kể cả DB bị lộ
 * cũng không tái dùng được token. Mỗi lần đăng nhập tạo một "family" mới;
 * mỗi lần refresh sẽ rotate trong cùng family. Nếu một token đã dùng/đã thu hồi
 * bị dùng lại ⇒ dấu hiệu bị đánh cắp ⇒ thu hồi toàn bộ family.</p>
 */
/*
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_rt_token_hash", columnList = "token_hash"),
    @Index(name = "idx_rt_family_id", columnList = "family_id")
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "token_hash", nullable = false, unique = true)
    String tokenHash;

    @Column(name = "family_id", nullable = false)
    UUID familyId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "expiry_time", nullable = false)
    Instant expiryTime;

    @Column(nullable = false)
    boolean used;

    @Column(nullable = false)
    boolean revoked;
}
*/