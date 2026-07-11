package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "AiErrorLogEntry", description = "Single AI error log entry for admin review.")
public class AiErrorLogEntry {

    @Schema(description = "Error timestamp.", example = "2026-07-11T14:23:01")
    LocalDateTime createdAt;

    @Schema(description = "Error message content.", example = "JSON parse error at line 42")
    String errorMessage;
}
