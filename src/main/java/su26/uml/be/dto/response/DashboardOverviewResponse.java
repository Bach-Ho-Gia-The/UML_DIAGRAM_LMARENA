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
@Schema(name = "DashboardOverviewResponse", description = "Aggregated SaaS-vital metrics for the admin overview (users, revenue, AI).")
public class DashboardOverviewResponse {

    @Schema(description = "User health metrics.")
    UserMetrics users;

    @Schema(description = "Revenue metrics (MRR, churn, ARPU, margin).")
    RevenueMetrics revenue;

    @Schema(description = "AI cost metrics for the selected period (0 until AI generation logging lands).")
    AiMetrics ai;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserMetrics {
        @Schema(description = "Total user count (all statuses).", example = "3200")
        long total;

        @Schema(description = "Daily Active Users (last 24h by last_active_at).", example = "412")
        long dau;

        @Schema(description = "Monthly Active Users (last 30d by last_active_at).", example = "1850")
        long mau;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RevenueMetrics {
        @Schema(description = "Monthly Recurring Revenue = sum of active plan prices.", example = "1250.00")
        BigDecimal mrr;

        @Schema(description = "Churn rate (%) = ended subscriptions in period / active at period start.", example = "2.3")
        double churnRate;

        @Schema(description = "Average Revenue Per User = MRR / active users.", example = "0.68")
        BigDecimal arpu;

        @Schema(description = "SaaS Margin = MRR - AI cost in period.", example = "1250.00")
        BigDecimal margin;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AiMetrics {
        @Schema(description = "Total AI chat requests in period.", example = "0")
        long requests;

        @Schema(description = "Total AI cost (USD) in period.", example = "0.00")
        BigDecimal costUsd;

        @Schema(description = "AI error rate (%) in period.", example = "0.0")
        double errorRate;

        @Schema(description = "Average AI latency (ms) in period.", example = "0.0")
        double avgLatencyMs;
    }
}
