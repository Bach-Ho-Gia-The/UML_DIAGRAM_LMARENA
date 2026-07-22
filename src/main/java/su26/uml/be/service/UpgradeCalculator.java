package su26.uml.be.service;

import org.springframework.stereotype.Component;
import su26.uml.be.dto.subscription.PlanSnapshot;
import su26.uml.be.dto.subscription.QuotaSnapshot;
import su26.uml.be.dto.subscription.SubscriptionSnapshot;
import su26.uml.be.dto.subscription.UpgradeQuoteResponse;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tính nâng cấp gói theo proration (thiết kế §1.7). Hàm THUẦN: 0 DB, 0 Spring dependency
 * (annotate {@code @Component} chỉ để inject; không giữ state). Dễ unit-test.
 *
 * <ul>
 *   <li>Tiền: {@code (giá đích − giá hiện tại) × tỉ lệ thời gian còn lại}, HALF_UP scale 0 (VND).</li>
 *   <li>Quota: {@code floor((nominal đích − hiện tại) × tỉ lệ kỳ quota còn lại)}.</li>
 * </ul>
 * Cả hai tính từ <b>duration thô</b> (giây), KHÔNG làm tròn tỉ lệ trước — tránh double-rounding
 * (vd 1/30 kỳ phải cho đúng floor, không lệch vì tỉ lệ bị cắt còn 6 chữ số).
 */
@Component
public class UpgradeCalculator {

    public UpgradeQuoteResponse calculate(SubscriptionSnapshot current, PlanSnapshot target,
                                          QuotaSnapshot quota, LocalDateTime now) {
        if (current.getTierOrder() == null || target.getTierOrder() == null) {
            throw new AppException(ErrorCode.PLAN_TIER_NOT_CONFIGURED);
        }
        if (target.getTierOrder() <= current.getTierOrder()) {
            throw new AppException(ErrorCode.UPGRADE_TARGET_NOT_HIGHER_TIER);
        }

        long billingTotal = seconds(current.getPeriodStart(), current.getPeriodEnd());
        long billingRemaining = clampRemaining(now, current.getPeriodEnd(), billingTotal);
        long quotaTotal = seconds(quota.getPeriodStart(), quota.getPeriodEnd());
        long quotaRemaining = clampRemaining(now, quota.getPeriodEnd(), quotaTotal);

        BigDecimal priceDifference = target.getPrice().subtract(current.getPrice());
        // amountToPay = priceDiff × billingRemaining / billingTotal (HALF_UP, scale 0)
        BigDecimal amountToPay = billingTotal <= 0
                ? BigDecimal.ZERO
                : priceDifference.multiply(BigDecimal.valueOf(billingRemaining))
                        .divide(BigDecimal.valueOf(billingTotal), 0, RoundingMode.HALF_UP);
        if (amountToPay.signum() < 0) {
            amountToPay = BigDecimal.ZERO; // downgrade tier khác đã bị chặn; guard giá lệch
        }

        int quotaDifference = target.getNominalAiLimit() - current.getNominalAiLimit();
        // quotaDelta = floor(quotaDiff × quotaRemaining / quotaTotal)
        int quotaDelta = quotaTotal <= 0
                ? 0
                : BigDecimal.valueOf((long) quotaDifference * quotaRemaining)
                        .divide(BigDecimal.valueOf(quotaTotal), 0, RoundingMode.FLOOR)
                        .intValue();
        if (quotaDelta < 0) {
            quotaDelta = 0;
        }
        int newEffectiveLimit = quota.getEffectiveLimit() + quotaDelta;
        int availableAfterUpgrade = Math.max(0, newEffectiveLimit - quota.getUsed());

        return UpgradeQuoteResponse.builder()
                .targetPlanId(target.getPlanId())
                .currentTierOrder(current.getTierOrder())
                .targetTierOrder(target.getTierOrder())
                .currency(target.getCurrency())
                .oldPrice(current.getPrice())
                .newPrice(target.getPrice())
                .priceDifference(priceDifference)
                .billingRemainingRatio(ratioForDisplay(billingRemaining, billingTotal))
                .quotaRemainingRatio(ratioForDisplay(quotaRemaining, quotaTotal))
                .billingRemainingDays((int) (billingRemaining / 86400))
                .billingTotalDays((int) (billingTotal / 86400))
                .amountToPay(amountToPay)
                .oldNominalQuota(current.getNominalAiLimit())
                .newNominalQuota(target.getNominalAiLimit())
                .quotaDelta(quotaDelta)
                .newEffectiveLimit(newEffectiveLimit)
                .availableAfterUpgrade(availableAfterUpgrade)
                .build();
    }

    private long seconds(LocalDateTime a, LocalDateTime b) {
        return Duration.between(a, b).getSeconds();
    }

    /** Giây còn lại từ now→end, kẹp về [0, total]. */
    private long clampRemaining(LocalDateTime now, LocalDateTime end, long total) {
        long remaining = seconds(now, end);
        if (remaining < 0) return 0;
        if (remaining > total) return total;
        return remaining;
    }

    /** Tỉ lệ hiển thị (scale 6) — chỉ để show cho FE, KHÔNG dùng để tính tiền/quota. */
    private BigDecimal ratioForDisplay(long remaining, long total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(remaining).divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }
}
