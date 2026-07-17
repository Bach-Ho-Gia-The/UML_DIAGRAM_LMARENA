package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(name = "user_name", nullable = false, unique = true)
    String username;

    @Column(name = "password")
    String password;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @Column(name = "email", nullable = false, unique = true)
    String email;

    @Column(name = "phone")
    String phone;

    @Column(name = "provider", length = 20)
    String provider;

    @Column(name = "google_id", unique = true)
    String googleId;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    UserStatus status;

    @Column(name = "deletion_date")
    LocalDateTime deletionDate;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "dob")
    LocalDate dob;

    @Column(name = "last_active_at")
    LocalDateTime lastActiveAt;

    @Column(name = "last_password_change_at")
    LocalDateTime lastPasswordChangeAt;

    // true = đã hoàn tất hồ sơ (đăng ký thường, hoặc Google user đã onboarding).
    // false = Google user đăng nhập lần đầu, chưa bổ sung thông tin → phải qua onboarding wizard.
    @Column(name = "profile_completed")
    Boolean profileCompleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_subscription_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Subscription currentSubscription;
}
