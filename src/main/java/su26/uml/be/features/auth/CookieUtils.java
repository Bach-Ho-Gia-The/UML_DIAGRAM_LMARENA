package su26.uml.be.features.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Quản lý refresh-token cookie (HttpOnly).
 *
 * <p>Refresh token KHÔNG còn được trả về trong body JSON hay nhét lên URL.
 * Nó được đặt trong một cookie {@code HttpOnly} nên JavaScript phía FE
 * không đọc được — kháng được tấn công đánh cắp token qua XSS.
 * Browser tự động đính kèm cookie này khi gọi {@code /auth/refresh} và
 * {@code /auth/logout}.</p>
 */
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CookieUtils {

    @NonFinal
    @Value("${app.auth.cookie.refresh-token-name:refresh_token}")
    String refreshCookieName;

    @NonFinal
    @Value("${app.auth.cookie.access-token-name:access_token}")
    String accessCookieName;

    /** Bật cho production (HTTPS). Trên http://localhost để false. */
    @NonFinal
    @Value("${app.auth.cookie.secure:false}")
    boolean secure;

    /** Lax | Strict | None. None bắt buộc đi kèm secure=true (cross-site). */
    @NonFinal
    @Value("${app.auth.cookie.same-site:Lax}")
    String sameSite;

    @NonFinal
    @Value("${app.auth.cookie.path:/}")
    String path;

    @NonFinal
    @Value("${jwt.refreshTokenExpiration:604800}")
    long refreshTokenExpiration;

    @NonFinal
    @Value("${jwt.accessTokenExpiration:3600}")
    long accessTokenExpiration;

    /** Đặt refresh token vào cookie HttpOnly trên response. */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, refreshCookieName, token, refreshTokenExpiration);
    }

    /** Đặt access token vào cookie HttpOnly trên response. */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, accessCookieName, token, accessTokenExpiration);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Xoá refresh-token cookie (khi logout). */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        clearCookie(response, refreshCookieName);
    }

    /** Xoá access-token cookie. */
    public void clearAccessTokenCookie(HttpServletResponse response) {
        clearCookie(response, accessCookieName);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Đọc refresh token từ cookie của request (nếu có). */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookie(request, refreshCookieName);
    }

    /** Đọc access token từ cookie của request (nếu có). */
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return extractCookie(request, accessCookieName);
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }
}