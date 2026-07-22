package su26.uml.be.features.auth.service;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenService {
    void store(String jti, String userId, Duration ttl);

    Optional<String> getUserIdByJti(String jti);

    void revoke(String jti);

    void revokeAllTokens(String userId);

    void setLogoutTime(String email);

    Long getLogoutTime(String email);
}