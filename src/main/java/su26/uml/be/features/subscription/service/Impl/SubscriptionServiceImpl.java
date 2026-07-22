package su26.uml.be.features.subscription.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.subscription.dto.MySubscriptionResponse;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.SubscriptionStatus;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.subscription.mapper.SubscriptionMapper;
import su26.uml.be.features.subscription.repository.SubscriptionRepository;
import su26.uml.be.features.user.repository.UserRepository;
import su26.uml.be.features.usage.service.QuotaService;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    UserRepository userRepository;
    SubscriptionRepository subscriptionRepository;
    SubscriptionAccessService subscriptionAccessService;
    QuotaService quotaService;
    SubscriptionMapper subscriptionMapper;

    @Override
    @Transactional
    public MySubscriptionResponse cancel(String email) {
        Subscription sub = requireActiveSubscription(email);
        if (Boolean.TRUE.equals(sub.getCancelAtPeriodEnd())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
        }
        // Giữ status ACTIVE, giữ endDate → Premium tới hết kỳ; chỉ đánh dấu không gia hạn.
        sub.setCancelAtPeriodEnd(true);
        sub.setCancelledAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
        return subscriptionMapper.toMySubscriptionResponse(sub);
    }

    @Override
    @Transactional
    public MySubscriptionResponse reactivate(String email) {
        Subscription sub = requireActiveSubscription(email);
        if (!Boolean.TRUE.equals(sub.getCancelAtPeriodEnd())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_CANCELLED);
        }
        sub.setCancelAtPeriodEnd(false);
        sub.setCancelledAt(null);
        subscriptionRepository.save(sub);
        return subscriptionMapper.toMySubscriptionResponse(sub);
    }

    @Override
    @Transactional(readOnly = true)
    public MySubscriptionResponse getMySubscription(String email) {
        UUID userId = userId(email);
        return subscriptionAccessService.getActiveSubscription(userId, LocalDateTime.now())
                .map(subscriptionMapper::toMySubscriptionResponse)
                .orElse(null); // đang ở base (không có sub) → FE ẩn phần hủy
    }

    @Override
    @Transactional
    public void expireEndedSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        for (Subscription sub : subscriptionRepository.findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now)) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            User u = sub.getUser();
            if (u.getCurrentSubscription() != null && u.getCurrentSubscription().getId().equals(sub.getId())) {
                u.setCurrentSubscription(null); // → về base (Free)
                userRepository.save(u);
                quotaService.resetOnPlanChange(u.getId()); // quota về base
            }
            log.info("Subscription {} của user {} hết kỳ → EXPIRED, về base", sub.getId(), u.getEmail());
        }
    }

    private Subscription requireActiveSubscription(String email) {
        return subscriptionAccessService.getActiveSubscription(userId(email), LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION));
    }

    private UUID userId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
                .getId();
    }
}