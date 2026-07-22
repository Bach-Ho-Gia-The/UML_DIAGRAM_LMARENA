package su26.uml.be.infrastructure.email;

import java.time.LocalDateTime;

public interface EmailService {
    void sendForgotPasswordOtpEmail(String toEmail, String otpCode, String fullName);
    void sendChangePasswordOtpEmail(String toEmail, String otpCode, String fullName);
    void sendAccountSetupSuccessEmail(String toEmail, String fullName, LocalDateTime setupTime);
}