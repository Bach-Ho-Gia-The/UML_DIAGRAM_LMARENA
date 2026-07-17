package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.PlanStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.service.RateLimiterService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RateLimiterServiceImpl implements RateLimiterService {

    StringRedisTemplate redis;
    SubscriptionRepository subscriptionRepository;
    PlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public void checkOrThrow(UUID userId, boolean isAdmin) {
        Integer per10s;
        Integer perMin;
        if (isAdmin) {
            per10s = planRepository.findMaxRatePer10s();
            perMin = planRepository.findMaxRatePerMin();
        } else {
            Plan plan = currentPlan(userId);
            per10s = plan == null ? null : plan.getRateLimitPer10s();
            perMin = plan == null ? null : plan.getRateLimitPerMin();
        }
        hit("rl:" + userId + ":10s", 10, per10s);
        hit("rl:" + userId + ":60s", 60, perMin);
    }

    /** INCR cửa sổ; lần đầu đặt TTL. Vượt ngưỡng → 429. Ngưỡng null/<=0 → bỏ qua (không giới hạn). */
    private void hit(String key, int ttlSeconds, Integer limit) {
        if (limit == null || limit <= 0) {
            return;
        }
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1L) {
            redis.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        if (count > limit) {
            log.warn("429 TOO_MANY_REQUESTS — key={} count={}/{} trong cửa sổ {}s", key, count, limit, ttlSeconds);
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
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
}
