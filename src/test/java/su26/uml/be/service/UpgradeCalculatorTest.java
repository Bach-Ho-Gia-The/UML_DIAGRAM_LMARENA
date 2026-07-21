package su26.uml.be.service;

import org.junit.jupiter.api.Test;
import su26.uml.be.dto.subscription.PlanSnapshot;
import su26.uml.be.dto.subscription.QuotaSnapshot;
import su26.uml.be.dto.subscription.SubscriptionSnapshot;
import su26.uml.be.dto.subscription.UpgradeQuoteResponse;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UpgradeCalculatorTest {

    final UpgradeCalculator calc = new UpgradeCalculator();
    final LocalDateTime now = LocalDateTime.of(2026, 7, 21, 12, 0);
    final UUID targetId = UUID.randomUUID();

    private SubscriptionSnapshot sub(Integer tier, long price, int nominal, LocalDateTime start, LocalDateTime end) {
        return SubscriptionSnapshot.builder()
                .tierOrder(tier).price(BigDecimal.valueOf(price)).currency("VND")
                .nominalAiLimit(nominal).periodStart(start).periodEnd(end).build();
    }

    private PlanSnapshot plan(Integer tier, long price, int nominal) {
        return PlanSnapshot.builder()
                .planId(targetId).tierOrder(tier).price(BigDecimal.valueOf(price)).currency("VND")
                .nominalAiLimit(nominal).build();
    }

    private QuotaSnapshot quota(int used, int effective, LocalDateTime start, LocalDateTime end) {
        return QuotaSnapshot.builder()
                .used(used).effectiveLimit(effective).periodStart(start).periodEnd(end).build();
    }

    // 1 — còn quota, kỳ 50%: Standard(600) → Pro(1500)
    @Test
    void case_hasQuota_50pct() {
        var start = now.minusDays(15);
        var end = now.plusDays(15);
        var r = calc.calculate(sub(2, 49000, 600, start, end), plan(3, 99000, 1500),
                quota(200, 600, start, end), now);
        assertEquals(450, r.getQuotaDelta());
        assertEquals(1050, r.getNewEffectiveLimit());
        assertEquals(850, r.getAvailableAfterUpgrade());
        assertEquals(0, new BigDecimal("25000").compareTo(r.getAmountToPay())); // 50000 × 0.5
    }

    // 2 — đã hết quota (used = effective)
    @Test
    void case_quotaExhausted() {
        var start = now.minusDays(15);
        var end = now.plusDays(15);
        var r = calc.calculate(sub(2, 49000, 600, start, end), plan(3, 99000, 1500),
                quota(600, 600, start, end), now);
        assertEquals(450, r.getQuotaDelta());
        assertEquals(450, r.getAvailableAfterUpgrade()); // 1050 - 600
    }

    // 3 — gần cuối kỳ 1/30 → delta phải đúng 30 (không lệch vì rounding tỉ lệ)
    @Test
    void case_oneThirtieth() {
        var start = now.minusDays(29);
        var end = now.plusDays(1);
        var r = calc.calculate(sub(2, 49000, 600, start, end), plan(3, 99000, 1500),
                quota(0, 600, start, end), now);
        assertEquals(30, r.getQuotaDelta()); // floor(900 × 1/30) = 30
    }

    // 4 — đầu kỳ 100% → full delta, full priceDiff
    @Test
    void case_fullPeriod() {
        var start = now;
        var end = now.plusDays(30);
        var r = calc.calculate(sub(2, 49000, 600, start, end), plan(3, 99000, 1500),
                quota(0, 600, start, end), now);
        assertEquals(900, r.getQuotaDelta());
        assertEquals(0, new BigDecimal("50000").compareTo(r.getAmountToPay()));
    }

    // 5 — kỳ đã hết hạn (now > end) → ratio 0 → delta 0, tiền 0
    @Test
    void case_expiredPeriod_clampZero() {
        var start = now.minusDays(31);
        var end = now.minusDays(1);
        var r = calc.calculate(sub(2, 49000, 600, start, end), plan(3, 99000, 1500),
                quota(100, 600, start, end), now);
        assertEquals(0, r.getQuotaDelta());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getAmountToPay()));
    }

    // 6 — hạ bậc (target tier < current) → reject
    @Test
    void case_downgrade_rejected() {
        var start = now.minusDays(15);
        var end = now.plusDays(15);
        var ex = assertThrows(AppException.class, () -> calc.calculate(
                sub(3, 99000, 1500, start, end), plan(2, 49000, 600), quota(0, 1500, start, end), now));
        assertEquals(ErrorCode.UPGRADE_TARGET_NOT_HIGHER_TIER, ex.getErrorCode());
    }

    // 7 — cùng bậc → reject
    @Test
    void case_sameTier_rejected() {
        var start = now.minusDays(15);
        var end = now.plusDays(15);
        var ex = assertThrows(AppException.class, () -> calc.calculate(
                sub(2, 49000, 600, start, end), plan(2, 49000, 600), quota(0, 600, start, end), now));
        assertEquals(ErrorCode.UPGRADE_TARGET_NOT_HIGHER_TIER, ex.getErrorCode());
    }

    // 8 — tierOrder chưa cấu hình (null) → reject
    @Test
    void case_tierNull_rejected() {
        var start = now.minusDays(15);
        var end = now.plusDays(15);
        var ex = assertThrows(AppException.class, () -> calc.calculate(
                sub(null, 49000, 600, start, end), plan(3, 99000, 1500), quota(0, 600, start, end), now));
        assertEquals(ErrorCode.PLAN_TIER_NOT_CONFIGURED, ex.getErrorCode());
    }

    // 9 — FLOOR quota: 1000 × 1/3 = 333.33 → 333
    @Test
    void case_floorQuota() {
        var start = now.minusDays(2);
        var end = now.plusDays(1);
        var r = calc.calculate(sub(2, 49000, 500, start, end), plan(3, 99000, 1500),
                quota(0, 500, start, end), now);
        assertEquals(333, r.getQuotaDelta());
    }

    // 10 — HALF_UP tiền: 100 × 2/3 = 66.67 → 67
    @Test
    void case_halfUpMoney() {
        var start = now.minusDays(1);
        var end = now.plusDays(2);
        var r = calc.calculate(sub(2, 0, 500, start, end), plan(3, 100, 1500),
                quota(0, 500, start, end), now);
        assertEquals(0, new BigDecimal("67").compareTo(r.getAmountToPay()));
    }
}
