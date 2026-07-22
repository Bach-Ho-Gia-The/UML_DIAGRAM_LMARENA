package su26.uml.be.features.auth.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import su26.uml.be.features.auth.service.TokenBlacklistService;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    static String KEY_PREFIX = "blacklist:";
    static String BLACKLISTED_VALUE = "1";

    RedisTemplate<String, String> redisTemplate;

    @Override
    public void blacklist(String jwtId, Date expiryTime) {
        if (jwtId == null || expiryTime == null) {
            return;
        }
        long ttlSeconds = (expiryTime.getTime() - System.currentTimeMillis()) / 1000;
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jwtId, BLACKLISTED_VALUE, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isBlacklisted(String jwtId) {
        if (jwtId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jwtId));
    }
}