package su26.uml.be.features.usage.service;

import java.util.UUID;

/**
 * Chống spam AI request — cửa sổ 10 giây và 1 phút (Redis). Ngưỡng đọc động theo gói của user;
 * admin dùng ngưỡng cao nhất. Không hardcode.
 */
public interface RateLimiterService {

    /** Ném {@code RATE_LIMIT_EXCEEDED} (429) nếu vượt ngưỡng 10s hoặc 60s. */
    void checkOrThrow(UUID userId, boolean isAdmin);
}