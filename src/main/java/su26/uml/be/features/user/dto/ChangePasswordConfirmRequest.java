package su26.uml.be.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordConfirmRequest {
    @NotBlank(message = "OTP_REQUIRED")
    String otpCode;

    @NotBlank(message = "PASSWORD_REQUIRED")
    String newPassword;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    String confirmPassword;
}