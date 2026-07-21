package su26.uml.be.service;

import su26.uml.be.dto.subscription.UpgradeQuoteResponse;

import java.util.UUID;

/**
 * Tạo báo giá nâng cấp gói (read-only, Chặng 1C). Ghép: gói effective hiện tại
 * ({@link SubscriptionAccessService}) + quota kỳ hiện tại ({@link QuotaPeriodService}) +
 * {@link UpgradeCalculator}. KHÔNG tạo payment, KHÔNG ghi state.
 */
public interface UpgradeQuoteService {

    /** Báo giá nâng lên {@code targetPlanId} cho user (theo email). Ném lỗi nếu không đủ điều kiện. */
    UpgradeQuoteResponse getQuote(String email, UUID targetPlanId);
}
