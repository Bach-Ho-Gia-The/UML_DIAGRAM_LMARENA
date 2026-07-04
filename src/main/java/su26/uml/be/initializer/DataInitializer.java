package su26.uml.be.initializer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.entity.Role;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.User;
import su26.uml.be.enums.UserStatus;
import su26.uml.be.repository.RoleRepository;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.UserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Order(2)
public class DataInitializer implements CommandLineRunner {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PlanRepository planRepository;
    PasswordEncoder passwordEncoder;
    JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data...");

        // 1. Initialize Roles
        Role adminRole = initRole("ADMIN", "System Administrator Role");
        Role userRole = initRole("USER", "Standard Application User Role");

        // 2. Initialize Admin User
        initAdminUser(adminRole);

        // 3. Initialize Plans
        initPlans();

        // 4. Backfill profile_completed for rows created before the column existed.
        backfillProfileCompleted();

        log.info("Data initialization completed.");
    }

    private void initPlans() {
        if (planRepository.count() == 0) {
            log.info("Initializing default plans via SQL...");
            String sql = "INSERT INTO plans (id, created_at, updated_at, name, price, description, duration_days, max_diagrams) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql, UUID.fromString("11111111-1111-1111-1111-111111111111"), "Free", 0.0, "For students and hobbyists.", -1, 3);
            jdbcTemplate.update(sql, UUID.fromString("22222222-2222-2222-2222-222222222222"), "Education", 3.0, "For education purposes.", 30, -1);
            jdbcTemplate.update(sql, UUID.fromString("33333333-3333-3333-3333-333333333333"), "Pro", 12.0, "For professional engineers, freelancers, and small product teams.", 30, -1);
            jdbcTemplate.update(sql, UUID.fromString("44444444-4444-4444-4444-444444444444"), "Enterprise", 24.0, "For large teams.", 30, -1);
            
            log.info("Plans initialized successfully.");
        } else {
            log.info("Updating existing plans to USD pricing...");
            String updateSql = "UPDATE plans SET price = CASE " +
                    "WHEN id = '11111111-1111-1111-1111-111111111111' THEN 0.0 " +
                    "WHEN id = '22222222-2222-2222-2222-222222222222' THEN 3.0 " +
                    "WHEN id = '33333333-3333-3333-3333-333333333333' THEN 12.0 " +
                    "WHEN id = '44444444-4444-4444-4444-444444444444' THEN 24.0 " +
                    "ELSE price END";
            jdbcTemplate.update(updateSql);
            log.info("Existing plans updated successfully.");
        }
    }

    /**
     * One-time backfill: rows that predate the {@code profile_completed} column come back as null.
     * Onboarding is a new feature, so no existing Google user has ever completed it — key on the
     * provider, not the password (the password is unreliable: fresh Google users may carry a random
     * BCrypt hash). A Google user becomes "completed" only once they have changed their own password
     * (onboarding/OTP reset sets {@code lastPasswordChangeAt}); everyone else (normal register) is
     * considered already complete.
     */
    private void backfillProfileCompleted() {
        var pending = userRepository.findAll().stream()
                .filter(u -> u.getProfileCompleted() == null)
                .toList();
        if (pending.isEmpty()) return;

        pending.forEach(u -> {
            boolean isGoogle = "GOOGLE".equalsIgnoreCase(u.getProvider());
            boolean completed = !isGoogle || u.getLastPasswordChangeAt() != null;
            u.setProfileCompleted(completed);
        });
        userRepository.saveAll(pending);
        log.info("Backfilled profile_completed for {} existing user(s).", pending.size());
    }

    private Role initRole(String roleName, String description) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .roleName(roleName)
                            .description(description)
                            .build();
                    log.info("Created role: {}", roleName);
                    return roleRepository.save(newRole);
                });
    }

    private void initAdminUser(Role adminRole) {
        String adminEmail = "admin@gmail.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            User adminUser = User.builder()
                    .email(adminEmail)
                    .username("admin")
                    .password(passwordEncoder.encode("Password123"))
                    .fullName("System Administrator")
                    .phone("0123456789")
                    .status(UserStatus.ACTIVE)
                    .profileCompleted(true)
                    .role(adminRole)
                    .build();
            userRepository.save(adminUser);
            log.info("Created sample admin user: {}", adminEmail);
        }
    }
}
