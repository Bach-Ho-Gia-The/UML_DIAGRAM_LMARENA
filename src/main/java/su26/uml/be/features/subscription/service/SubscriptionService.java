package su26.uml.be.features.subscription.service;

import su26.uml.be.features.subscription.dto.MySubscriptionResponse;

import java.util.UUID;

/**
 * Quản lý vòng đời subscription trả phí: hủy gia hạn (graceful), hoàn tác, hạ cấp đã đặt
 * (booked downgrade), và hết kỳ về base.
 */
public interface SubscriptionService {

    /** Hủy gia hạn: giữ Premium tới endDate rồi về base. KHÔNG thu hồi ngay. */
    MySubscriptionResponse cancel(String email);

    /** Hoàn tác hủy (trước khi hết kỳ). */
    MySubscriptionResponse reactivate(String email);

    /** Subscription trả phí hiện tại của user; null nếu đang ở base (không có sub). */
    MySubscriptionResponse getMySubscription(String email);

    /**
     * Đặt hạ cấp có kỳ hạn (booked downgrade — hướng A): ghi pendingPlanId + pendingEffectiveAt
     * (= endDate kỳ hiện tại). KHÔNG thu tiền, KHÔNG đổi quyền ngay — user vẫn dùng gói cũ tới
     * hết kỳ. Hạ về gói mặc định (isDefaultPlan) thì coi như hủy gia hạn (cancelAtPeriodEnd).
     */
    MySubscriptionResponse scheduleDowngrade(String email, UUID targetPlanId);

    /** Huỷ thay đổi đang chờ (pendingPlanId) — giữ nguyên gói hiện tại tới hết kỳ. */
    MySubscriptionResponse cancelPendingChange(String email);

    /** Job: sub ACTIVE quá endDate → EXPIRED + về base + reset quota (hủy lẫn hết hạn tự nhiên). */
    void expireEndedSubscriptions();
}