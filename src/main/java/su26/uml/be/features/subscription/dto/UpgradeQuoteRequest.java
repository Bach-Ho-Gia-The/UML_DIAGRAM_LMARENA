package su26.uml.be.features.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/** FE chỉ gửi gói đích; BE tự tính toàn bộ quote (BR-UPGRADE-09). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "UpgradeQuoteRequest")
public class UpgradeQuoteRequest {

    @NotNull(message = "PLAN_NOT_FOUND")
    @Schema(description = "ID gói muốn nâng lên", example = "44444444-4444-4444-4444-444444444444")
    UUID targetPlanId;
}