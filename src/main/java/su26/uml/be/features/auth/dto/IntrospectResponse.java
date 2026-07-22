package su26.uml.be.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "IntrospectResponse", description = "Result of token introspection.")
public class IntrospectResponse {
    @Schema(description = "True when the supplied token is valid and not blacklisted.", example = "true")
    boolean valid;
}