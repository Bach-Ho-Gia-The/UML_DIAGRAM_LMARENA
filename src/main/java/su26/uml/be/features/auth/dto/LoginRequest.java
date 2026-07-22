package su26.uml.be.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "LoginRequest", description = "Credentials used to obtain a JWT.")
public class LoginRequest {
    @NotBlank(message = "EMAIL_REQUIRED")
    @Schema(description = "Account email.", example = "johndoe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Schema(description = "Account password.", example = "Passw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    String password;
}