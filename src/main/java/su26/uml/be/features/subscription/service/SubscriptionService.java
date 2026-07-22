package su26.uml.be.features.subscription.service;

import su26.uml.be.features.subscription.dto.MySubscriptionResponse;

/**
 * Quản lý vòng đời subscription trả phí: hủy gia hạn (graceful), hoàn tác, và hết kỳ về base.
 */
public interface SubscriptionService {

    /** Hủy gia hạn: giữ Premium tới endDate rồi về base. KHÔNG thu hồi ngay. */
    MySubscriptionResponse cancel(String email);

    /** Hoàn tác hủy (trước khi hết kỳ). */
    MySubscriptionResponse reactivate(String email);

    /** Subscription trả phí hiện tại của user; null nếu đang ở base (không có sub). */
    MySubscriptionResponse getMySubscription(String email);

    /** Job: sub ACTIVE quá endDate → EXPIRED + về base + reset quota (hủy lẫn hết hạn tự nhiên). */
    void expireEndedSubscriptions();
}