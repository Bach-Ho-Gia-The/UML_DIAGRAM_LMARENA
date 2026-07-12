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
        applyLazyReset(q);
        // Atomic: chỉ trừ khi còn quota (hoặc unlimited). flushAutomatically đẩy lazy-reset xuống DB trước.
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
        applyLazyReset(q);
        return QuotaResponse.builder()
                .used(q.getAiUsed())
                .limit(q.getAiLimit())
                .resetAt(q.getResetAt())
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
        q.setAiLimit(aiLimitOf(currentPlan(userId)));
        q.setResetAt(LocalDateTime.now().plusDays(periodDays));
        userQuotaRepository.save(q);
    }

    // --- helpers ---

    private UserQuota getOrCreate(UUID userId) {
        return userQuotaRepository.findByUserId(userId).orElseGet(() -> {
            // Đua tạo song song (2 request đầu tiên cùng lúc) rất hiếm → nếu xảy ra sẽ ném
            // DataIntegrityViolation và request đó fail 1 lần, client retry là có row.
            Plan plan = currentPlan(userId);
            return userQuotaRepository.save(UserQuota.builder()
                    .userId(userId)
                    .aiLimit(aiLimitOf(plan))
                    .resetAt(LocalDateTime.now().plusDays(periodDays))
                    .build());
        });
    }

    /** Reset lười khi hết kỳ: về 0 + cập nhật limit theo gói hiện tại + đẩy reset_at tới mốc tương lai. */
    private void applyLazyReset(UserQuota q) {
        LocalDateTime now = LocalDateTime.now();
        if (q.getResetAt() != null && !q.getResetAt().isAfter(now)) {
            LocalDateTime next = q.getResetAt();
            while (!next.isAfter(now)) {
                next = next.plusDays(periodDays);
            }
            q.setAiUsed(0);
            q.setExportUsed(0);
            q.setAiLimit(aiLimitOf(currentPlan(q.getUserId())));
            q.setResetAt(next);
            userQuotaRepository.save(q);
        }
    }

    /** Gói hiện tại: subscription ACTIVE → gói; nếu không có → gói ACTIVE giá thấp nhất. */
    private Plan currentPlan(UUID userId) {
        return subscriptionRepository
                .findFirstByUser_IdAndStatusOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE)
                .map(s -> s.getPlan())
                .orElseGet(() -> planRepository
                        .findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE)
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
