package su26.uml.be.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.service.JwtService;
import su26.uml.be.service.RefreshTokenService;
import su26.uml.be.service.TokenBlacklistService;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenRedis;
    private final CookieUtils cookieUtils;
    private final TokenBlacklistService tokenBlacklistService;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.contains("/auth/refresh") || path.contains("/auth/login") || path.contains("/users/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = cookieUtils.extractAccessToken(request).orElse(null);

        // Bonus for fun (Swagger support)
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JWTClaimsSet claims = jwtService.parseClaims(token);

            String typ = claims.getStringClaim("typ");
            if (!"access".equals(typ)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (tokenBlacklistService.isBlacklisted(claims.getJWTID())) {
                log.warn("Token is blacklisted: {}", claims.getJWTID());
                cookieUtils.clearAccessTokenCookie(response);
                cookieUtils.clearRefreshTokenCookie(response);
                filterChain.doFilter(request, response);
                return;
            }

            String email = claims.getSubject();

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long logoutTime = refreshTokenRedis.getLogoutTime(email);
                long issuedAt = claims.getIssueTime().getTime();
                if (logoutTime != null && issuedAt < logoutTime) {
                    log.warn("Token has been revoked due to logout or password change for user: {}", email);
                    cookieUtils.clearAccessTokenCookie(response);
                    cookieUtils.clearRefreshTokenCookie(response);
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.error("Failed to authenticate JWT token: {}", e.getMessage());
            cookieUtils.clearAccessTokenCookie(response);
            cookieUtils.clearRefreshTokenCookie(response);
            handlerExceptionResolver.resolveException(request, response, null, new su26.uml.be.exception.AppException(su26.uml.be.exception.ErrorCode.UNAUTHENTICATED));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
