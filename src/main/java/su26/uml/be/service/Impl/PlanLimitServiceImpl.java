package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.PlanStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.service.PlanLimitService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlanLimitServiceImpl implements PlanLimitService {

    SubscriptionRepository subscriptionRepository;
    PlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertCanCreate(UUID userId, PlanFeatureKey key, long currentCount) {
        int limit = limitOf(currentPlan(userId), key);
        if (limit == -1) {
            return; // unlimited
        }
        // limit 0 (chưa đặt / không có gói) → chặn ngay.
        if (currentCount >= limit) {
            throw new AppException(ErrorCode.PLAN_LIMIT_EXCEEDED);
        }
    }

    /** Gói hiện tại: subscription ACTIVE (chưa hết hạn) → gói; nếu không có → gói ACTIVE giá thấp nhất. */
    private Plan currentPlan(UUID userId) {
        return subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now())
                .map(Subscription::getPlan)
                .orElseGet(() -> planRepository
                        .findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE)
                        .orElse(null));
    }

    /** Giá trị limit của key; null (chưa đặt) / không có gói → 0 (chặn). -1 = unlimited. */
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
