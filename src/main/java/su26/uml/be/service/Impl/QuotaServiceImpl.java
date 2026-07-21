package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.dto.response.QuotaResponse;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;
import su26.uml.be.entity.Subscription;
import su26.uml.be.entity.UserQuota;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.PlanStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserQuotaRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.QuotaService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuotaServiceImpl implements QuotaService {

    @org.springframework.beans.factory.annotation.Value("${quota.period-days:30}")
    @lombok.experimental.NonFinal
    int periodDays;

    UserQuotaRepository userQuotaRepository;
    SubscriptionRepository subscriptionRepository;
    PlanRepository planRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public void reserveAiRequest(UUID userId) {
        UserQuota q = getOrCreate(userId);
        syncQuotaToCurrentPlan(q, userId);
        // Atomic: chỉ trừ khi còn quota (hoặc unlimited). flushAutomatically đẩy sync xuống DB trước.
        int updated = userQuotaRepository.tryReserveAi(userId);
        if (updated == 0) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED);
        }
    }

    @Override
    @Transactional
    public void rollbackAiRequest(UUID userId) {
        userQuotaRepository.rollbackAi(userId);
    }

    @Override
    @Transactional
    public QuotaResponse getQuota(UUID userId) {
        UserQuota q = getOrCreate(userId);
        syncQuotaToCurrentPlan(q, userId);
        return QuotaResponse.builder()
                .used(q.getAiUsed())
                .limit(q.getAiLimit())
                .resetAt(q.getResetAt())
                .nominalLimit(q.getNominalAiLimit())
                .effectiveLimit(q.getEffectiveAiLimit())
                .periodStart(q.getQuotaPeriodStart())
                .periodEnd(q.getQuotaPeriodEnd())
                .planId(q.getPlanId())
                .subscriptionId(q.getSubscriptionId())
                .build();
    }

    @Override
    @Transactional
    public QuotaResponse getMyQuota(String email) {
        UUID uid = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
                .getId();
        return getQuota(uid);
    }

    @Override
    @Transactional
    public void resetOnPlanChange(UUID userId) {
        UserQuota q = getOrCreate(userId);
        q.setAiUsed(0);
        q.setExportUsed(0);
        applyPlanSnapshot(q, userId);
    }

    @Override
    @Transactional
    public void applyUpgrade(UUID userId, int newEffectiveLimit, UUID subscriptionId) {
        UserQuota q = getOrCreate(userId);
        q.setAiLimit(newEffectiveLimit);
        q.setEffectiveAiLimit(newEffectiveLimit);
        q.setSubscriptionId(subscriptionId);
        // planId sẽ được cập nhật ở lần syncQuotaToCurrentPlan hoặc applyPlanSnapshot kế tiếp.
        // KHÔNG đụng aiUsed và resetAt — giữ used & kỳ hiện tại (BR-UPGRADE-05/06).
        userQuotaRepository.save(q);
    }

    // --- helpers ---

    private UserQuota getOrCreate(UUID userId) {
        return userQuotaRepository.findByUserId(userId).orElseGet(() -> {
            UserQuota q = UserQuota.builder()
                    .userId(userId)
                    .aiLimit(0)
                    .build();
            applyPlanSnapshot(q, userId);
            return q;
        });
    }

    /** Đồng bộ quota theo subscription hiện tại: reset nếu đổi sub hoặc hết kỳ. */
    private void syncQuotaToCurrentPlan(UserQuota q, UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        var subOpt = subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE, now);
        UUID newSubId = subOpt.map(Subscription::getId).orElse(null);
        boolean periodExpired = q.getResetAt() != null && !q.getResetAt().isAfter(now);
        boolean subChanged = !Objects.equals(newSubId, q.getSubscriptionId());

        if (periodExpired || subChanged) {
            q.setAiUsed(0);
            q.setExportUsed(0);
            applyPlanSnapshot(q, userId); // sẽ save trong này
        }
    }

    /** Snapshot plan hiện tại vào quota (limit, subscriptionId, resetAt, + Chặng 2B fields) — không reset used. */
    private void applyPlanSnapshot(UserQuota q, UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        var subOpt = subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE, now);
        Plan plan = subOpt.map(Subscription::getPlan).orElseGet(() ->
                planRepository.findFirstByIsBasePlanTrueAndStatus(PlanStatus.ACTIVE)
                        .or(() -> planRepository.findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE))
                        .orElse(null));
        int limit = aiLimitOf(plan);
        q.setAiLimit(limit);
        q.setSubscriptionId(subOpt.map(Subscription::getId).orElse(null));
        // Paid: reset theo hết hạn subscription (kỳ billing). Gói tier thấp nhất (Free, không có sub):
        // reset lăn mỗi `periodDays` ngày kể từ bây giờ — dùng chung cơ chế, không còn mốc "vô cực".
        LocalDateTime periodEnd = subOpt.map(Subscription::getEndDate).orElse(now.plusDays(periodDays));
        q.setResetAt(periodEnd);
        // Chặng 2B: populate new fields đồng bộ với old fields
        q.setNominalAiLimit(limit);
        q.setEffectiveAiLimit(limit);
        q.setQuotaPeriodStart(now);
        q.setQuotaPeriodEnd(periodEnd);
        q.setPlanId(plan != null ? plan.getId() : null);
        userQuotaRepository.save(q);
    }

    /** Gói hiện tại: subscription ACTIVE (chưa hết hạn) → gói; nếu không có → gói base (isBasePlan), fallback giá thấp nhất. */
    private Plan currentPlan(UUID userId) {
        return subscriptionRepository
                .findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now())
                .map(s -> s.getPlan())
                .orElseGet(() -> planRepository
                        .findFirstByIsBasePlanTrueAndStatus(PlanStatus.ACTIVE)
                        .or(() -> planRepository.findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE))
                        .orElse(null));
    }

    /** AI_QUERIES của gói; null (chưa đặt) / không có gói → 0 (chặn). -1 = unlimited. */
    private int aiLimitOf(Plan plan) {
        if (plan == null) {
            return 0;
        }
        return plan.getPlanFeatures().stream()
                .filter(f -> f.getFeatureKey() == PlanFeatureKey.AI_QUERIES)
                .map(PlanFeature::getLimitValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(0);
    }
}
