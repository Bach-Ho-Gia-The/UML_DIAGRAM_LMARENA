package su26.uml.be.features.user.service;

public interface OtpService {

    void storeOtp(String email, String otpCode);
    void verifyOtp(String email, String otpCode);
    void markVerified(String email);
    boolean isVerified(String email);
    void invalidate(String email);
}