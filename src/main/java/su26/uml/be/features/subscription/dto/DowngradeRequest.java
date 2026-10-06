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

/** Body cho POST /subscriptions/downgrade — đặt hạ cấp có kỳ hạn (booked downgrade). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "DowngradeRequest")
public class DowngradeRequest {

    @NotNull(message = "PLAN_NOT_FOUND")
    @Schema(description = "Gói muốn hạ xuống (phải ACTIVE, bậc thấp hơn gói hiện tại, không phải gói báo giá). "
            + "Hạ về gói mặc định = hủy gia hạn.", example = "22222222-2222-2222-2222-222222222222")
    UUID targetPlanId;
}
