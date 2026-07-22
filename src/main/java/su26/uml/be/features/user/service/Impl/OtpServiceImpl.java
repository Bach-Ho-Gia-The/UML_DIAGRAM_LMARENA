package su26.uml.be.features.user.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.user.service.OtpService;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpServiceImpl implements OtpService {

    static String OTP_PREFIX = "pwd_otp:";
    static String ATTEMPT_PREFIX = "pwd_otp_attempt:";
    static String VERIFIED_PREFIX = "pwd_otp_verified:";

    StringRedisTemplate redis;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${otp.ttl-seconds:90}")
    long otpTtlSeconds;

    @NonFinal
    @Value("${otp.max-attempts:5}")
    long maxAttempts;

    @NonFinal
    @Value("${otp.verified-ttl-seconds:300}")
    long verifiedTtlSeconds;

    @Override
    public void storeOtp(String email, String otpCode) {
        String hash = passwordEncoder.encode(otpCode);
        redis.opsForValue().set(OTP_PREFIX + email, hash, Duration.ofSeconds(otpTtlSeconds));
        // Cấp OTP mới → xoá bộ đếm sai và cờ verified của lần trước.
        redis.delete(List.of(ATTEMPT_PREFIX + email, VERIFIED_PREFIX + email));
    }

    @Override
    public void verifyOtp(String email, String otpCode) {
        String hash = redis.opsForValue().get(OTP_PREFIX + email);
        if (hash == null) {
            // Không tồn tại hoặc đã hết hạn TTL.
            throw new AppException(ErrorCode.OTP_NOT_FOUND);
        }

        if (passwordEncoder.matches(otpCode, hash)) {
            return; // hợp lệ
        }

        // Sai mã → tăng bộ đếm, khoá khi vượt ngưỡng.
        Long attempts = redis.opsForValue().increment(ATTEMPT_PREFIX + email);
        if (attempts != null && attempts == 1L) {
            redis.expire(ATTEMPT_PREFIX + email, Duration.ofSeconds(otpTtlSeconds));
        }

        if (attempts != null && attempts >= maxAttempts) {
            // Đốt OTP để chặn brute-force tiếp; người dùng phải xin mã mới.
            invalidate(email);
            throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }

        throw new AppException(ErrorCode.INVALID_OTP_OR_EMAIL);
    }

    @Override
    public void markVerified(String email) {
        redis.opsForValue().set(VERIFIED_PREFIX + email, "1", Duration.ofSeconds(verifiedTtlSeconds));
    }

    @Override
    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redis.hasKey(VERIFIED_PREFIX + email));
    }

    @Override
    public void invalidate(String email) {
        redis.delete(List.of(OTP_PREFIX + email, ATTEMPT_PREFIX + email, VERIFIED_PREFIX + email));
    }
}