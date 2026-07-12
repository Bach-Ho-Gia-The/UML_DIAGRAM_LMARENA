package su26.uml.be.service;

import su26.uml.be.enums.PlanFeatureKey;

import java.util.UUID;

/**
 * Enforce hạn mức resource theo gói tại điểm tạo (Projects, Diagrams…).
 * Khác quota AI: không rate-limit, không rollback — chỉ chặn khi vượt.
 */
public interface PlanLimitService {

    /**
     * Ném {@code PLAN_LIMIT_EXCEEDED} nếu tạo thêm sẽ vượt hạn mức gói.
     * @param currentCount số resource user đang có.
     */
    void assertCanCreate(UUID userId, PlanFeatureKey key, long currentCount);
}
