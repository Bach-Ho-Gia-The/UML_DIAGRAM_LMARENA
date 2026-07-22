package su26.uml.be.features.usage.service;

import su26.uml.be.features.usage.dto.QuotaResponse;
import su26.uml.be.features.plan.entity.Plan;

import java.util.UUID;

/**
 * Quản lý ví quota AI Request (1 ví / user, reset 30 ngày).
 * Flow: reserve (trước khi gọi AI) → nếu AI fail thì rollback.
 */
public interface QuotaService {

    /** Trừ 1 AI request (atomic). Ném {@code QUOTA_EXCEEDED} (402) nếu hết lượt. */
    void reserveAiRequest(UUID userId);

    /** Hoàn 1 AI request (gọi khi AI thất bại). */
    void rollbackAiRequest(UUID userId);

    /** Trạng thái quota để hiển thị (đã áp lazy reset). */
    QuotaResponse getQuota(UUID userId);

    /** Như getQuota nhưng resolve từ email (cho controller). */
    QuotaResponse getMyQuota(String email);

    /** Reset quota khi user đổi gói (mua/nâng/hạ/hết hạn). */
    void resetOnPlanChange(UUID userId);

    /**
     * Áp nâng cấp giữa kỳ: cập nhật ngay {@code planId} + {@code nominalAiLimit} theo {@code newPlan},
     * đặt limit hiệu lực = {@code newEffectiveLimit}, gắn {@code subscriptionId} mới; NHƯNG giữ nguyên
     * {@code aiUsed} và {@code resetAt} (BR-UPGRADE-05/06 — không reset used, không kéo dài kỳ).
     */
    void applyUpgrade(UUID userId, Plan newPlan, int newEffectiveLimit, UUID subscriptionId);
}