package su26.uml.be.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Kết quả tính nâng cấp (read-only). Số tiền/quota do BE tính, FE chỉ hiển thị & xác nhận. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "UpgradeQuoteResponse")
public class UpgradeQuoteResponse {
    UUID targetPlanId;
    Integer currentTierOrder;
    Integer targetTierOrder;
    String currency;

    BigDecimal oldPrice;
    BigDecimal newPrice;
    BigDecimal priceDifference;
    BigDecimal billingRemainingRatio;
    BigDecimal quotaRemainingRatio;

    /** Số ngày còn lại trong kỳ thanh toán (dùng hiển thị "x / y ngày"). */
    int billingRemainingDays;
    /** Tổng số ngày của kỳ thanh toán. */
    int billingTotalDays;

    /** Số tiền phải trả cho phần còn lại của kỳ (đã prorated, HALF_UP scale 0). */
    BigDecimal amountToPay;

    int oldNominalQuota;
    int newNominalQuota;
    int quotaDelta;
    int newEffectiveLimit;
    /** Lượt AI còn dùng được ngay sau khi nâng = max(0, newEffectiveLimit - used). */
    int availableAfterUpgrade;

    LocalDateTime quoteExpiresAt;
}
