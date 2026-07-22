package su26.uml.be.features.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AuthenticationResponse", description = "Result of a successful login.")
public class AuthenticationResponse {
    @Schema(description = "Signed HS512 JWT. Use as 'Authorization: Bearer <token>'.",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huZG9lIn0...")
    String token;

    @Schema(description = "True when authentication succeeded.", example = "true")
    boolean authenticated;

    @Schema(description = "Internal only — delivered via HttpOnly cookie, omitted from the response body.",
            hidden = true)
    String refreshToken;
}