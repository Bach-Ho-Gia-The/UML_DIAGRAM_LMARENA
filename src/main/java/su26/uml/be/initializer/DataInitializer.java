package su26.uml.be.initializer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.entity.FeatureCatalog;
import su26.uml.be.entity.Role;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.User;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.UserStatus;
import su26.uml.be.repository.FeatureCatalogRepository;
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
    FeatureCatalogRepository featureCatalogRepository;
    PasswordEncoder passwordEncoder;
    JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data...");

        // 0. Ensure the ShedLock coordination table exists. It is NOT a JPA entity, so
        //    ddl-auto=update never creates it, and the JdbcTemplate lock provider does not
        //    self-create schema — without this the scheduled jobs fail with
        //    "relation \"shedlock\" does not exist".
        initShedLockTable();

        // 1. Initialize Roles
        Role adminRole = initRole("ADMIN", "System Administrator Role");
        Role userRole = initRole("USER", "Standard Application User Role");

        // 2. Initialize Admin User
        initAdminUser(adminRole);

        // 3. Initialize Plans
        initPlans();

        // 3b. Initialize starter feature catalog (admin can add/edit/delete afterwards).
        initFeatureCatalog();

        // 3c. Seed quota limits (plan_features) + rate limits for the seed plans (idempotent —
        //     ON CONFLICT DO NOTHING for limits, only-if-null for rate limits → admin edits preserved).
        seedPlanQuotasAndRateLimits();

        // 4. Backfill profile_completed for rows created before the column existed.
        backfillProfileCompleted();

        log.info("Data initialization completed.");
    }

    private void seedPlanQuotasAndRateLimits() {

        // Rate limit (per 10s / per minute)
        setRate("11111111-1111-1111-1111-111111111111", 3, 15);      // Free
        setRate("22222222-2222-2222-2222-222222222222", 6, 30);      // Education
        setRate("33333333-3333-3333-3333-333333333333", 8, 45);      // Standard
        setRate("44444444-4444-4444-4444-444444444444", 12, 80);     // Pro
//        setRate("55555555-5555-5555-5555-555555555555", null, null); // Enterprise

        // AI, Projects, Diagrams, Export PDF, Collaborators
        seedLimits("11111111-1111-1111-1111-111111111111", 50, 3, 15, 5, 1);

        seedLimits("22222222-2222-2222-2222-222222222222", 400, 12, 30, 50, 4);

        seedLimits("33333333-3333-3333-3333-333333333333", 600, 20, 60, 65, 6);

        seedLimits("44444444-4444-4444-4444-444444444444", 1500, 30, 100, 100, 10);

//        seedLimits("55555555-5555-5555-5555-555555555555", -1, -1, -1, -1, -1);

        log.info("Plan quotas & rate limits seeded (idempotent).");
    }

    private void setRate(String planId, Integer per10s, Integer perMin) {
        jdbcTemplate.update(
                "UPDATE plans SET rate_limit_per_10s = ?, rate_limit_per_min = ? "
                        + "WHERE id = ? AND rate_limit_per_10s IS NULL AND rate_limit_per_min IS NULL",
                per10s, perMin, UUID.fromString(planId));
    }

    private void seedLimits(String planId, int ai, int projects, int diagrams, int exportPdf, int collaborators) {
        seedFeature(planId, PlanFeatureKey.AI_QUERIES, ai);
        seedFeature(planId, PlanFeatureKey.MAX_PROJECTS, projects);
        seedFeature(planId, PlanFeatureKey.MAX_DIAGRAMS, diagrams);
        seedFeature(planId, PlanFeatureKey.EXPORT_PDF, exportPdf);
        seedFeature(planId, PlanFeatureKey.MAX_COLLABORATORS, collaborators);
    }

    private void seedFeature(String planId, PlanFeatureKey key, int value) {
        jdbcTemplate.update(
                "INSERT INTO plan_features (id, created_at, updated_at, plan_id, feature_key, limit_value) "
                        + "VALUES (?, now(), now(), ?, ?, ?) ON CONFLICT (plan_id, feature_key) DO NOTHING",
                UUID.randomUUID(), UUID.fromString(planId), key.name(), value);
    }

    private void initShedLockTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shedlock (" +
                "name VARCHAR(64) NOT NULL PRIMARY KEY, " +
                "lock_until TIMESTAMP NOT NULL, " +
                "locked_at TIMESTAMP NOT NULL, " +
                "locked_by VARCHAR(255) NOT NULL)");
        log.info("ShedLock table ensured.");
    }

    private void initPlans() {
        if (planRepository.count() == 0) {
            log.info("Initializing default plans via SQL (VND, ACTIVE)...");

            String sql = """
                INSERT INTO plans (
                    id,
                    created_at,
                    updated_at,
                    name,
                    price,
                    currency,
                    status,
                    description,
                    duration_days,
                    max_diagrams
                )
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 'VND', 'ACTIVE', ?, ?, ?)
                """;

            // Free
            jdbcTemplate.update(
                    sql,
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Free",
                    0,
                    "For students and hobbyists.",
                    -1,
                    15);

            // Education
            jdbcTemplate.update(
                    sql,
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "Education",
                    29000,
                    "Student plan (requires student verification).",
                    30,
                    30);

            // Standard
            jdbcTemplate.update(
                    sql,
                    UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    "Standard",
                    49000,
                    "For individual users.",
                    30,
                    60);

            // Pro
            jdbcTemplate.update(
                    sql,
                    UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    "Pro",
                    99000,
                    "For developers, business analysts and professionals.",
                    30,
                    100);

            log.info("Plans initialized successfully.");
        } else {
            log.info("Migrating existing seed plans...");

            String updateSql = """
                UPDATE plans
                SET
                    price = CASE
                        WHEN id = '11111111-1111-1111-1111-111111111111' THEN 0
                        WHEN id = '22222222-2222-2222-2222-222222222222' THEN 29000
                        WHEN id = '33333333-3333-3333-3333-333333333333' THEN 49000
                        WHEN id = '44444444-4444-4444-4444-444444444444' THEN 99000
                        ELSE price
                    END,
                    max_diagrams = CASE
                        WHEN id = '11111111-1111-1111-1111-111111111111' THEN 15
                        WHEN id = '22222222-2222-2222-2222-222222222222' THEN 30
                        WHEN id = '33333333-3333-3333-3333-333333333333' THEN 60
                        WHEN id = '44444444-4444-4444-4444-444444444444' THEN 100
                        ELSE max_diagrams
                    END,
                    currency = 'VND',
                    status = 'ACTIVE'
                WHERE id IN (
                    '11111111-1111-1111-1111-111111111111',
                    '22222222-2222-2222-2222-222222222222',
                    '33333333-3333-3333-3333-333333333333',
                    '44444444-4444-4444-4444-444444444444'
                )
                AND (currency IS NULL OR currency <> 'VND')
                """;

            jdbcTemplate.update(updateSql);

            log.info("Existing seed plans migrated.");
        }
    }

    private void initFeatureCatalog() {
        if (featureCatalogRepository.count() > 0) {
            return;
        }
        log.info("Seeding starter feature catalog...");
        String[] labels = {
                "Vẽ diagram",
                "Xuất PDF",
                "Cộng tác realtime",
                "Chia sẻ công khai",
                "Ưu tiên hỗ trợ"
        };
        for (int i = 0; i < labels.length; i++) {
            featureCatalogRepository.save(FeatureCatalog.builder()
                    .label(labels[i])
                    .sortOrder(i + 1)
                    .build());
        }
        log.info("Feature catalog seeded ({} features).", labels.length);
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
                    .password(passwordEncoder.encode("Admin123"))
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
