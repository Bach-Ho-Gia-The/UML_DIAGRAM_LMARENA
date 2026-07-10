package su26.uml.be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class CoreActivityTracker {

    private static final String DAU_KEY_PREFIX = "hll:dau:";

    private final StringRedisTemplate redisTemplate;

    public void trackActivity(String userEmail) {
        String key = DAU_KEY_PREFIX + LocalDate.now(ZoneId.of("UTC"));
        redisTemplate.opsForHyperLogLog().add(key, userEmail);
    }

    public long getDailyActiveUsers() {
        String key = DAU_KEY_PREFIX + LocalDate.now(ZoneId.of("UTC"));
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0;
    }

    public long getMonthlyActiveUsers() {
        String mergeKey = "hll:mau:merge";
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        try {
            redisTemplate.opsForHyperLogLog().union(mergeKey, getLast30DayKeys(today));
            Long count = redisTemplate.opsForHyperLogLog().size(mergeKey);
            return count != null ? count : 0;
        } finally {
            redisTemplate.delete(mergeKey);
        }
    }

    private String[] getLast30DayKeys(LocalDate today) {
        return java.util.stream.Stream.iterate(today.minusDays(29), d -> d.plusDays(1))
                .limit(30)
                .map(d -> DAU_KEY_PREFIX + d)
                .toArray(String[]::new);
    }
}
