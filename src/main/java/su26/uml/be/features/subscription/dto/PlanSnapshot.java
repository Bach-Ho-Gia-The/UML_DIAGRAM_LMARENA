package su26.uml.be.features.subscription.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

/** Snapshot gói đích khi nâng cấp — input cho {@link su26.uml.be.service.UpgradeCalculator}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanSnapshot {
    UUID planId;
    Integer tierOrder;
    BigDecimal price;
    String currency;
    int nominalAiLimit;
}