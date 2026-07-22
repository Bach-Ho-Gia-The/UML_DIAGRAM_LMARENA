package su26.uml.be.service;

import su26.uml.be.dto.subscription.QuotePairResponse;
import su26.uml.be.dto.subscription.UpgradeQuoteResponse;
import su26.uml.be.enums.UpgradeMode;

import java.util.UUID;

/**
 * Tạo báo giá nâng cấp gói (read-only, Chặng 1C). Ghép: gói effective hiện tại
 * ({@link SubscriptionAccessService}) + quota kỳ hiện tại ({@link QuotaPeriodService}) +
 * {@link UpgradeCalculator}. KHÔNG tạo payment, KHÔNG ghi state.
 */
public interface UpgradeQuoteService {

    /** Báo giá nâng lên {@code targetPlanId} cho 1 mode cụ thể (PRORATED hoặc DIRECT). */
    UpgradeQuoteResponse getQuote(String email, UUID targetPlanId, UpgradeMode mode);

    /** Báo giá cả 2 mode (PRORATED + DIRECT) trong 1 lần gọi. */
    QuotePairResponse getQuotePair(String email, UUID targetPlanId);
}
