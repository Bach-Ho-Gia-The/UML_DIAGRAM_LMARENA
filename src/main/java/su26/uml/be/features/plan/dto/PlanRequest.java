package su26.uml.be.features.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import su26.uml.be.common.constant.enums.PlanStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "PlanRequest", description = "Create/update payload for a subscription plan.")
public class PlanRequest {

    @NotBlank(message = "PLAN_NAME_REQUIRED")
    @Schema(example = "Pro")
    String name;

    @Schema(example = "For power users")
    String description;

    @NotNull(message = "PLAN_PRICE_REQUIRED")
    @PositiveOrZero(message = "PLAN_PRICE_INVALID")
    @Schema(example = "300000")
    BigDecimal price;

    @Schema(description = "Currency code; defaults to VND when omitted.", example = "VND")
    String currency;

    @Schema(description = "active | draft | archived; defaults to draft when omitted.", example = "active")
    PlanStatus status;

    @Schema(example = "true")
    Boolean popular;

    @Schema(description = "Accent color (hex).", example = "#7C3AED")
    String color;

    @Schema(example = "true")
    Boolean yearlyBilling;

    @Schema(description = "Yearly discount percentage.", example = "20")
    Integer yearlyDiscount;

    @Schema(description = "Show \"Liên hệ báo giá\" instead of a price (Enterprise). Distinct from price = 0.", example = "false")
    Boolean contactSales;

    @Schema(description = "Billing period length in days.", example = "30")
    Integer durationDays;

    @Schema(description = "Rate limit — requests per 10 seconds (technical, hidden from public /plans).", example = "15")
    Integer rateLimitPer10s;

    @Schema(description = "Rate limit — requests per minute (technical, hidden from public /plans).", example = "100")
    Integer rateLimitPerMin;

    @Schema(description = "Numeric usage limits. Use -1 for unlimited.")
    PlanLimitsRequest limits;

    @Schema(description = "IDs of catalog features enabled for this plan (from GET /admin/features). "
            + "Features not listed here show as ✗ in the comparison matrix.",
            example = "[\"a1b2c3d4-...\", \"e5f6a7b8-...\"]")
    List<UUID> enabledFeatureIds;

    @Schema(description = "Mark as the base/fallback plan (only one allowed).", example = "true")
    Boolean isBasePlan;

    @Schema(description = "Tier ordering: 0 = base, 1, 2, 3... Must be unique across plans.", example = "0")
    Integer tierOrder;

    @Schema(description = "Billing cycle (MONTHLY / YEARLY).", example = "MONTHLY")
    String billingCycle;

    @Schema(description = "Quota period in days.", example = "30")
    Integer quotaPeriodDays;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(name = "PlanLimitsRequest", description = "Numeric usage limits (-1 = unlimited).")
    public static class PlanLimitsRequest {
        Integer projects;
        Integer diagrams;
        Integer aiQueries;
        Integer exportPdf;
        Integer collaborators;
    }
}