package su26.uml.be.features.plan.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.plan.entity.PlanFeature;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.plan.service.PlanResolutionService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlanResolutionServiceImpl implements PlanResolutionService {

    SubscriptionAccessService subscriptionAccessService;
    PlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> resolveEffectivePlan(UUID userId) {
        return subscriptionAccessService.getActiveSubscription(userId, LocalDateTime.now())
                .map(Subscription::getPlan)
                .or(this::defaultPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> resolveEffectivePlanFor(User user) {
        if (user == null) {
            return Optional.empty();
        }
        return resolveEffectivePlan(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Plan requireDefaultPlan() {
        return defaultPlan().orElseThrow(() -> new AppException(ErrorCode.NO_DEFAULT_PLAN));
    }

    /** Gói mặc định: isDefaultPlan = true VÀ status = ACTIVE. Không fallback theo giá (D5). */
    private Optional<Plan> defaultPlan() {
        return planRepository.findFirstByStatusAndIsDefaultPlanTrue(PlanStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Entitlements resolveEntitlements(UUID userId) {
        return entitlementsOf(
                subscriptionAccessService.getActiveSubscription(userId, LocalDateTime.now()).orElse(null));
    }

    @Override
    public Entitlements entitlementsOf(Subscription subOrNull) {
        if (subOrNull != null) {
            Entitlements snapshot = fromSnapshot(subOrNull);
            if (snapshot != null) {
                return snapshot;
            }
            // Sub cũ (trước khi có entitlement snapshot) → live plan của sub.
            return fromPlan(subOrNull.getPlan());
        }
        Plan def = defaultPlan().orElse(null);
        return def != null ? fromPlan(def) : Entitlements.none();
    }

    /** {@code null} nếu sub chưa có snapshot (sub tạo trước cơ chế này). */
    private Entitlements fromSnapshot(Subscription sub) {
        if (sub.getSnapshotPlanName() == null && sub.getSnapshotAiQueries() == null) {
            return null;
        }
        return new Entitlements(
                sub.getSnapshotAiQueries(),
                sub.getSnapshotMaxProjects(),
                sub.getSnapshotMaxDiagrams(),
                sub.getSnapshotMaxExportPdf(),
                sub.getSnapshotMaxCollaborators(),
                sub.getSnapshotRateLimitPer10s(),
                sub.getSnapshotRateLimitPerMin());
    }

    private Entitlements fromPlan(Plan plan) {
        if (plan == null) {
            return Entitlements.none();
        }
        return new Entitlements(
                limitOf(plan, PlanFeatureKey.AI_QUERIES),
                limitOf(plan, PlanFeatureKey.MAX_PROJECTS),
                limitOf(plan, PlanFeatureKey.MAX_DIAGRAMS),
                limitOf(plan, PlanFeatureKey.EXPORT_PDF),
                limitOf(plan, PlanFeatureKey.MAX_COLLABORATORS),
                plan.getRateLimitPer10s(),
                plan.getRateLimitPerMin());
    }

    private int limitOf(Plan plan, PlanFeatureKey key) {
        if (plan == null) {
            return 0;
        }
        return plan.getPlanFeatures().stream()
                .filter(f -> f.getFeatureKey() == key)
                .map(PlanFeature::getLimitValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(0);
    }
}
