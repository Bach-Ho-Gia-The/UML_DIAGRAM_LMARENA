package su26.uml.be.features.subscription.service;

import su26.uml.be.features.subscription.entity.Subscription;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Nguồn phân giải "gói paid đang hiệu lực" tập trung (thiết kế §1.3) — tránh đọc
 * {@code user.currentSubscription} rải rác (planing.md R4). Additive: chỉ read.
 */
public interface SubscriptionAccessService {

    /** Paid subscription effective (status ACTIVE, startDate ≤ now < endDate), mới nhất theo endDate. */
    Optional<Subscription> getActiveSubscription(UUID userId, LocalDateTime now);
}