package su26.uml.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_saas_metrics")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailySaasMetric extends BaseEntity {

    @Column(name = "snapshot_date", nullable = false, unique = true)
    LocalDate snapshotDate;

    @Column(name = "total_users", nullable = false)
    @Builder.Default
    long totalUsers = 0;

    @Column(name = "mau", nullable = false)
    @Builder.Default
    long mau = 0;

    @Column(name = "mrr", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal mrr = BigDecimal.ZERO;

    @Column(name = "churn_rate", nullable = false)
    @Builder.Default
    double churnRate = 0;

    @Column(name = "arpu", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal arpu = BigDecimal.ZERO;

    @Column(name = "ai_requests", nullable = false)
    @Builder.Default
    long aiRequests = 0;

    @Column(name = "ai_cost_usd", precision = 12, scale = 6, nullable = false)
    @Builder.Default
    BigDecimal aiCostUsd = BigDecimal.ZERO;

    @Column(name = "ai_error_rate", nullable = false)
    @Builder.Default
    double aiErrorRate = 0;

    @Column(name = "ai_avg_latency_ms", nullable = false)
    @Builder.Default
    double aiAvgLatencyMs = 0;
}
