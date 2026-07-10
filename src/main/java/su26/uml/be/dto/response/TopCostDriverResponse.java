package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "TopCostDriverResponse", description = "A user ranked by total AI cost.")
public class TopCostDriverResponse {

    @Schema(description = "User id (UUID).", example = "a1b2c3d4-...")
    String userId;

    @Schema(description = "User full name.", example = "Nguyen Van A")
    String fullName;

    @Schema(description = "User email.", example = "a@example.com")
    String email;

    @Schema(description = "Number of AI requests made by this user (ranking key).", example = "120")
    long requestCount;

    @Schema(description = "Total tokens consumed by this user.", example = "90400")
    long totalTokens;

    @Schema(description = "Total AI cost (USD). Synthetic/near-zero while AI runs locally (Ollama).", example = "0.00")
    BigDecimal totalCostUsd;
}
