package su26.uml.be.features.auth.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import su26.uml.be.features.auth.service.RefreshTokenService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final String PREFIX = "rt:";
    private static final String USER_INDEX_PREFIX = "user_rt:";
    private static final String LOGOUT_TIME_PREFIX = "logout_time:";
    private final StringRedisTemplate redis;

    @Override
    public void store(String jti, String userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + jti, userId, ttl);

        String indexKey = USER_INDEX_PREFIX + userId;
        redis.opsForSet().add(indexKey, jti);
        redis.expire(indexKey, ttl);
    }

    @Override
    public Optional<String> getUserIdByJti(String jti) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + jti));
    }

    @Override
    public void revoke(String jti) {
        String userId = redis.opsForValue().get(PREFIX + jti);
        if (userId != null) {
            redis.delete(PREFIX + jti);
            redis.opsForSet().remove(USER_INDEX_PREFIX + userId, jti);
        }
    }

    @Override
    public void revokeAllTokens(String userId) {
        String indexKey = USER_INDEX_PREFIX + userId;
        Set<String> jtis = redis.opsForSet().members(indexKey);

        if (jtis != null && !jtis.isEmpty()) {
            List<String> keysToDelete = new ArrayList<>();
            for (String jti : jtis) {
                keysToDelete.add(PREFIX + jti);
            }
            redis.delete(keysToDelete);
            redis.delete(indexKey);
        }
    }

    // Cắm mốc thời gian lúc đổi pass/logout-all
    @Override
    public void setLogoutTime(String email) {
        redis.opsForValue().set(LOGOUT_TIME_PREFIX + email, String.valueOf(System.currentTimeMillis()));
    }

    // Lấy mốc thời gian để so sánh
    @Override
    public Long getLogoutTime(String email) {
        String val = redis.opsForValue().get(LOGOUT_TIME_PREFIX + email);
        return val != null ? Long.parseLong(val) : null;
    }
}