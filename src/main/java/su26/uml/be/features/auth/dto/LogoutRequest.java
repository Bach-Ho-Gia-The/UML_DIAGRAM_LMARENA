package su26.uml.be.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "LogoutRequest", description = "Wraps the JWT to invalidate.")
public class LogoutRequest {
    @Schema(description = "The JWT to invalidate (blacklist until expiry).", example = "eyJhbGciOiJIUzUxMiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    String token;
}