package su26.uml.be.features.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordInitRequest {
    @NotBlank(message = "PASSWORD_REQUIRED")
    String currentPassword;
}