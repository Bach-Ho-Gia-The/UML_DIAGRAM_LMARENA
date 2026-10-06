package su26.uml.be.features.plan.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.user.entity.User;
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
}
