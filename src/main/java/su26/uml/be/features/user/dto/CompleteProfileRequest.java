package su26.uml.be.features.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "CompleteProfileRequest",
        description = "Payload for the first-time onboarding of an OAuth2 (Google) user: " +
                "personal info + a self-chosen password. Only callable while the profile is not yet completed.")
public class CompleteProfileRequest {

    @NotBlank(message = "FULLNAME_REQUIRED")
    @Size(max = 255, message = "INVALID_FULLNAME")
    @Schema(description = "Display name, up to 255 characters.", example = "Nguyen Van A",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String fullName;

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "INVALID_PHONE")
    @Schema(description = "Phone number, 10-11 digits.", example = "0901234567",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String phone;

    @NotNull(message = "DOB_REQUIRED")
    @Past(message = "INVALID_DOB")
    @Schema(description = "Date of birth, must be in the past.", example = "2000-01-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate dob;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "WEAK_PASSWORD")
    @Schema(description = "Self-chosen password. Min 8 chars; server enforces at least 'medium' strength.",
            example = "Passw0rd!", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
    String password;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    @Schema(description = "Must equal password.", example = "Passw0rd!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String confirmPassword;
}