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
@Schema(name = "AiModelStatsResponse", description = "AI error & usage stats grouped by provider/model.")
public class AiModelStatsResponse {

    @Schema(description = "AI provider name (e.g. openai, deepseek).", example = "openai")
    String provider;

    @Schema(description = "Model name (e.g. gpt-4o, deepseek-chat).", example = "gpt-4o")
    String modelName;

    @Schema(description = "Total AI requests in period.", example = "12847")
    long totalRequests;

    @Schema(description = "Failed request count.", example = "23")
    long errorCount;

    @Schema(description = "Error rate (%) = errorCount / totalRequests × 100.", example = "0.18")
    double errorRate;

    @Schema(description = "Total cost (USD) in period.", example = "4.20")
    BigDecimal totalCostUsd;

    @Schema(description = "Total tokens consumed in period.", example = "1250000")
    long totalTokens;
}
