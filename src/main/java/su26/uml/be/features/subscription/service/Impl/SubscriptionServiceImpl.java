package su26.uml.be.features.subscription.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.subscription.dto.MySubscriptionResponse;
import su26.uml.be.features.subscription.entity.Subscription;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.common.constant.enums.SubscriptionStatus;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.subscription.mapper.SubscriptionMapper;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.subscription.repository.SubscriptionRepository;
import su26.uml.be.features.user.repository.UserRepository;
import su26.uml.be.features.usage.service.QuotaService;
import su26.uml.be.features.subscription.service.SubscriptionAccessService;
import su26.uml.be.features.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.Objects;
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
    PlanRepository planRepository;

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
        LocalDateTime now = LocalDateTime.now();
        // Còn sub ACTIVE → trả sub đó (kèm pending nếu đang có hạ cấp đã đặt).
        // Hết kỳ rồi nhưng vẫn còn pendingPlanId (T23 giữ nguyên) → trả kèm pending để FE hiện banner.
        return subscriptionAccessService.getActiveSubscription(userId, now)
                .map(this::toResponse)
                .or(() -> subscriptionRepository
                        .findFirstByUser_IdAndPendingPlanIdIsNotNullOrderByEndDateDesc(userId)
                        .map(this::toResponse))
                .orElse(null); // đang ở base (không có sub) → FE ẩn phần hủy
    }

    @Override
    @Transactional
    public MySubscriptionResponse scheduleDowngrade(String email, UUID targetPlanId) {
        Subscription sub = requireActiveSubscription(email);

        Plan target = planRepository.findById(targetPlanId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
        if (target.getStatus() != PlanStatus.ACTIVE) {
            // Không cho đặt hạ xuống gói DRAFT/ARCHIVED.
            throw new AppException(ErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }
        if (target.isContactSales()) {
            throw new AppException(ErrorCode.PLAN_CONTACT_SALES_REQUIRED);
        }

        Plan currentPlan = sub.getPlan();
        if (Objects.equals(currentPlan.getId(), target.getId())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }
        if (target.getTierOrder() == null || currentPlan.getTierOrder() == null) {
            throw new AppException(ErrorCode.PLAN_TIER_NOT_CONFIGURED);
        }
        if (target.getTierOrder() >= currentPlan.getTierOrder()) {
            // Không phải hạ gói → user nhầm endpoint (nâng gói dùng API upgrade).
            throw new AppException(ErrorCode.PLAN_SAME_TIER);
        }

        // Hạ về gói MẶC ĐỊNH (Free) = hành vi hủy gia hạn: hết kỳ về base, không đặt pending.
        if (Boolean.TRUE.equals(target.getIsDefaultPlan())) {
            if (Boolean.TRUE.equals(sub.getCancelAtPeriodEnd())) {
                throw new AppException(ErrorCode.SUBSCRIPTION_ALREADY_CANCELLED);
            }
            sub.setPendingPlanId(null);
            sub.setPendingEffectiveAt(null);
            sub.setCancelAtPeriodEnd(true);
            sub.setCancelledAt(LocalDateTime.now());
            Subscription saved = subscriptionRepository.save(sub);
            log.info("User {} đặt hạ về gói mặc định {} (cancelAtPeriodEnd)", email, target.getName());
            return toResponse(saved);
        }

        // Hạ xuống gói trả phí bậc thấp hơn: BOOKED DOWNGRADE — ghi pending, không thu tiền,
        // không đổi quyền. User vẫn dùng gói cũ tới hết kỳ; kích hoạt lại qua payment + pendingPlanId.
        sub.setPendingPlanId(target.getId());
        sub.setPendingEffectiveAt(sub.getEndDate());
        Subscription saved = subscriptionRepository.save(sub);
        log.info("User {} đặt hạ gói: {} -> {} (effective {})",
                email, currentPlan.getName(), target.getName(), sub.getPendingEffectiveAt());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MySubscriptionResponse cancelPendingChange(String email) {
        UUID userId = userId(email);
        LocalDateTime now = LocalDateTime.now();

        // Tìm thay đổi đang chờ: ưu tiên sub ACTIVE, không thì row pending gần nhất (EXPIRED).
        Subscription found = subscriptionAccessService.getActiveSubscription(userId, now)
                .filter(s -> s.getPendingPlanId() != null)
                .or(() -> subscriptionRepository
                        .findFirstByUser_IdAndPendingPlanIdIsNotNullOrderByEndDateDesc(userId))
                .orElseThrow(() -> new AppException(ErrorCode.NO_PENDING_PLAN_CHANGE));

        // Clear TOÀN BỘ row pending của user (kể cả row EXPIRED cũ) để banner không bám lại.
        subscriptionRepository.clearPendingForUser(userId);
        log.info("User {} huỷ thay đổi gói đang chờ (pendingPlanId={})", email, found.getPendingPlanId());

        // Sau bulk update, persistence context đã bị clear → đọc lại trạng thái mới.
        Subscription activeNow = subscriptionAccessService.getActiveSubscription(userId, now).orElse(null);
        if (activeNow != null) {
            return toResponse(activeNow);
        }
        return subscriptionRepository.findById(found.getId())
                .map(this::toResponse)
                .orElseGet(() -> toResponse(found));
    }

    @Override
    @Transactional
    public void expireEndedSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        for (Subscription sub : subscriptionRepository.findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now)) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            // T23: GIỮ pendingPlanId/pendingEffectiveAt trên row EXPIRED — GET /me/subscription
            // đọc row này để hiện banner "gói sẽ chuyển sang X", user bấm thanh toán pendingPlanId.
            subscriptionRepository.save(sub);

            User u = sub.getUser();
            if (u.getCurrentSubscription() != null && u.getCurrentSubscription().getId().equals(sub.getId())) {
                u.setCurrentSubscription(null); // → về base (Free)
                userRepository.save(u);
                quotaService.resetOnPlanChange(u.getId()); // quota về base (T24: requireDefaultPlan)
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

    /** Map sub → response, tra tên/giá gói pending để FE hiện banner hạ cấp đã đặt. */
    private MySubscriptionResponse toResponse(Subscription sub) {
        MySubscriptionResponse r = subscriptionMapper.toMySubscriptionResponse(sub);
        if (sub.getPendingPlanId() != null) {
            planRepository.findById(sub.getPendingPlanId()).ifPresent(pending -> {
                r.setPendingPlanName(pending.getName());
                r.setPendingPrice(pending.getPrice());
            });
        }
        return r;
    }
}
