package su26.uml.be.features.plan.service;

import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.user.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Nguồn DUY NHẤT phân giải "gói hiện lực" của user (thay cho 7 bản sao logic fallback rải rác
 * trước đây — UserServiceImpl, CustomOAuth2UserServiceImpl, QuotaServiceImpl x2,
 * PlanLimitServiceImpl, RateLimiterServiceImpl).
 *
 * <p>Thứ tự ưu tiên (D9):
 * <ol>
 *   <li>Subscription paid đang hiệu lực (status ACTIVE, startDate ≤ now &lt; endDate) → gói của sub đó.</li>
 *   <li>Không có → gói mặc định: {@code isDefaultPlan = true} và {@code status = ACTIVE}.</li>
 * </ol>
 *
 * <p>KHÔNG suy luận theo giá (price = 0) hay tierOrder = 0 (D5) — chỉ dùng cờ isDefaultPlan.
 * Không có side-effect: không tạo subscription, không ghi quota (D9 — user mới không tạo Subscription).
 */
public interface PlanResolutionService {

    /** Gói hiện lực của user; rỗng nếu user không có sub VÀ hệ thống chưa cấu hình default plan. */
    Optional<Plan> resolveEffectivePlan(UUID userId);

    /** Như {@link #resolveEffectivePlan(UUID)} nhưng nhận sẵn entity User (các call-site đã load user). */
    Optional<Plan> resolveEffectivePlanFor(User user);

    /** Gói mặc định ACTIVE; ném {@code NO_DEFAULT_PLAN} nếu hệ thống chưa cấu hình (fail loud). */
    Plan requireDefaultPlan();

    /**
     * Quyền lợi HIỆU LỰC của user — nguồn duy nhất cho quota / limits / rate-limit.
     *
     * <p>Thứ tự ưu tiên:
     * <ol>
     *   <li>Subscription đang hiệu lực CÓ entitlement snapshot → đọc snapshot (quyền lợi đóng băng
     *       lúc mua — admin sửa gói sau đó không ảnh hưởng user đang dùng).</li>
     *   <li>Sub cũ chưa có snapshot → live plan của sub.</li>
     *   <li>Không có sub → live plan mặc định.</li>
     *   <li>Không có sub và không có default plan → {@link Entitlements#none()}.</li>
     * </ol>
     */
    Entitlements resolveEntitlements(UUID userId);

    /** Như {@link #resolveEntitlements(UUID)} nhưng nhận sẵn sub (call-site đã load — tránh query lại). */
    Entitlements entitlementsOf(Subscription subOrNull);

    /**
     * Quyền lợi dạng bất biến. Limit {@code null} = chưa cấu hình (coi như 0 = chặn);
     * {@code -1} = unlimited; rate-limit {@code null}/&le;0 = không giới hạn.
     */
    record Entitlements(Integer aiQueries, Integer maxProjects, Integer maxDiagrams,
                        Integer maxExportPdf, Integer maxCollaborators,
                        Integer rateLimitPer10s, Integer rateLimitPerMin) {

        /** Không có sub VÀ không có default plan: mọi limit = 0 (chặn), rate-limit = null (không giới hạn). */
        public static Entitlements none() {
            return new Entitlements(0, 0, 0, 0, 0, null, null);
        }
    }
}
