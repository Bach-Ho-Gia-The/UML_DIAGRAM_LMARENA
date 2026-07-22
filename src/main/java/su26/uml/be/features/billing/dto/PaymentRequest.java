package su26.uml.be.features.billing.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import su26.uml.be.common.constant.enums.UpgradeMode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private UUID planId;
    private String returnUrl;
    private String cancelUrl;

    @Builder.Default
    private UpgradeMode upgradeMode = UpgradeMode.PRORATED;
}