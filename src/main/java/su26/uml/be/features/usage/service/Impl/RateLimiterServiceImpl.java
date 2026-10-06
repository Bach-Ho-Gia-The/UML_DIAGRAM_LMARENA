package su26.uml.be.features.usage.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.plan.service.PlanResolutionService;
import su26.uml.be.features.usage.service.RateLimiterService;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RateLimiterServiceImpl implements RateLimiterService {

    StringRedisTemplate redis;
    PlanResolutionService planResolutionService;
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
            // Rate limit cũng theo entitlement snapshot: admin sửa gói (kể cả ACTIVE/ARCHIVED)
            // không dịch chuyển ngưỡng mà user đang chịu cho tới hết kỳ.
            PlanResolutionService.Entitlements e = planResolutionService.resolveEntitlements(userId);
            per10s = e.rateLimitPer10s();
            perMin = e.rateLimitPerMin();
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

}