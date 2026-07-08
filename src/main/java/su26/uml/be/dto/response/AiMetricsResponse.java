package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "AiMetricsResponse", description = "AI usage & cost metrics for the admin dashboard.")
public class AiMetricsResponse {

    @Schema(description = "Total AI requests in period.", example = "0")
    long totalRequests;

    @Schema(description = "Total tokens consumed.", example = "0")
    long totalTokens;

    @Schema(description = "Average latency per request (ms).", example = "0.0")
    double avgLatencyMs;

    @Schema(description = "Error rate (%) in period.", example = "0.0")
    double errorRate;

    @Schema(description = "Total cost (USD) in period.", example = "0.00")
    BigDecimal totalCostUsd;

    @Schema(description = "Daily cost breakdown for charts.")
    List<CostByDay> costByDay;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(name = "CostByDay", description = "Per-day AI cost entry.")
    public static class CostByDay {
        @Schema(description = "Date (yyyy-MM-dd).", example = "2026-07-01")
        String date;

        @Schema(description = "Total cost on this day (USD).", example = "0.00")
        BigDecimal cost;

        @Schema(description = "Provider name.", example = "openai")
        String provider;
    }
}
