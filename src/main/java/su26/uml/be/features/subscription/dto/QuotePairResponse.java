package su26.uml.be.features.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "QuotePairResponse")
public class QuotePairResponse {

    @Schema(description = "Báo giá nâng cấp tiết kiệm (prorated)")
    UpgradeQuoteResponse prorated;

    @Schema(description = "Báo giá nâng cấp thẳng (direct)")
    UpgradeQuoteResponse direct;
}