package su26.uml.be.service.scheduleJobs.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.DailySaasMetric;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.enums.UserStatus;
import su26.uml.be.repository.AiGenerationLogRepository;
import su26.uml.be.repository.DailySaasMetricRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.scheduleJobs.SaasMetricSyncService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaasMetricSyncServiceImpl implements SaasMetricSyncService {

    private static final String DAU_KEY_PREFIX = "hll:dau:";
    private static final int MAU_DAYS = 30;
    private static final int TTL_DAYS = 32;

    private final StringRedisTemplate redisTemplate;
    private final DailySaasMetricRepository metricRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;

    @Override
    public void syncMetrics() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDateTime last30d = now.minusDays(MAU_DAYS);

        long totalUsers = userRepository.countByStatusAndRoleRoleName(UserStatus.ACTIVE, "USER");
        long mau = computeMau(today);

        long activeBefore30d = subscriptionRepository.countByStartDateBeforeAndStatus(last30d, SubscriptionStatus.ACTIVE);
        long churned30d = subscriptionRepository.countByStatusAndEndDateBetween(SubscriptionStatus.EXPIRED, last30d, now)
                + subscriptionRepository.countByStatusAndEndDateBetween(SubscriptionStatus.CANCELLED, last30d, now);
        double churnRate = activeBefore30d == 0 ? 0 : Math.round((double) churned30d / activeBefore30d * 100 * 10.0) / 10.0;

        BigDecimal mrr = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).stream()
                .map(s -> s.getPlan().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal arpu = totalUsers == 0 ? BigDecimal.ZERO
                : mrr.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP);

        long aiRequests = aiGenerationLogRepository.countByCreatedAtBetween(last30d, now);
        long aiErrors = aiGenerationLogRepository.countByCreatedAtBetweenAndSuccess(last30d, now, false);
        double aiErrorRate = aiRequests == 0 ? 0 : Math.round((double) aiErrors / aiRequests * 100 * 10.0) / 10.0;

        DailySaasMetric metric = DailySaasMetric.builder()
                .snapshotDate(today)
                .totalUsers(totalUsers)
                .mau(mau)
                .mrr(mrr)
                .churnRate(churnRate)
                .arpu(arpu)
                .aiRequests(aiRequests)
                .aiCostUsd(BigDecimal.ZERO)
                .aiErrorRate(aiErrorRate)
                .aiAvgLatencyMs(0)
                .build();

        metricRepository.save(metric);
        refreshHllTtl(today);

        log.info("DailySaasMetric synced: date={}, mau={}, mrr={}, churn={}%", today, mau, mrr, churnRate);
    }

    private long computeMau(LocalDate today) {
        String mergeKey = "hll:mau:merge";
        try {
            redisTemplate.opsForHyperLogLog().union(mergeKey, getLast30DayKeys(today));
            Long count = redisTemplate.opsForHyperLogLog().size(mergeKey);
            return count != null ? count : 0;
        } finally {
            redisTemplate.delete(mergeKey);
        }
    }

    private void refreshHllTtl(LocalDate today) {
        Stream.iterate(today, d -> d.minusDays(1))
                .limit(TTL_DAYS)
                .map(d -> DAU_KEY_PREFIX + d)
                .forEach(key -> redisTemplate.expire(key, Duration.ofDays(TTL_DAYS)));
    }

    private String[] getLast30DayKeys(LocalDate today) {
        return Stream.iterate(today.minusDays(MAU_DAYS - 1), d -> d.plusDays(1))
                .limit(MAU_DAYS)
                .map(d -> DAU_KEY_PREFIX + d)
                .toArray(String[]::new);
    }
}
