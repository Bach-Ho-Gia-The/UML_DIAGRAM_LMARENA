package su26.uml.be.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import su26.uml.be.enums.PlanStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "PlanResponse", description = "A subscription plan with its limits, features and live subscriber count.")
public class PlanResponse {

    String id;
    String name;
    String description;

    @Schema(example = "300000")
    BigDecimal price;

    @Schema(example = "VND")
    String currency;

    @Schema(example = "active")
    PlanStatus status;

    boolean popular;

    @Schema(example = "#7C3AED")
    String color;

    boolean yearlyBilling;

    @Schema(example = "20")
    Integer yearlyDiscount;

    @Schema(description = "Show \"Liên hệ báo giá\" instead of price (Enterprise).", example = "false")
    boolean contactSales;

    @Schema(example = "30")
    Integer durationDays;

    @Schema(description = "Number of active subscriptions on this plan.", example = "42")
    long subscribers;

    @Schema(description = "Numeric usage limits (-1 = unlimited).")
    PlanLimits limits;

    @Schema(description = "Full feature catalog with an included flag per row — render the comparison matrix directly. "
            + "included=false → show ✗. Rows are identical/ordered across all plans so columns align.")
    List<FeatureCell> features;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(name = "PlanLimits", description = "Numeric usage limits (-1 = unlimited).")
    public static class PlanLimits {
        Integer projects;
        Integer diagrams;
        Integer aiQueries;
        Integer collaborators;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(name = "FeatureCell", description = "One row of the comparison matrix for this plan.")
    public static class FeatureCell {
        @Schema(description = "Catalog feature id.")
        String id;

        @Schema(description = "Feature label.", example = "Vẽ diagram")
        String label;

        @Schema(description = "Whether this plan includes the feature (false → ✗).", example = "true")
        boolean included;
    }
}
