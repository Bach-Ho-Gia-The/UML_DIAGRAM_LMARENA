package su26.uml.be.features.plan.service;

import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.features.plan.dto.PlanRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.plan.dto.PlanResponse;

import java.util.List;
import java.util.UUID;

public interface PlanService {
    ApiResponse<List<PlanResponse>> getPublicPlans();
    ApiResponse<List<PlanResponse>> getAllPlans();
    ApiResponse<PlanResponse> getPlan(UUID id);
    ApiResponse<PlanResponse> createPlan(PlanRequest request);
    ApiResponse<PlanResponse> updatePlan(UUID id, PlanRequest request);

    /**
     * Admin đổi trạng thái gói (endpoint riêng, không nằm trong PlanRequest).
     * Bảng chuyển trạng thái: DRAFT→ACTIVE (publish), DRAFT→ARCHIVED, ACTIVE→ARCHIVED,
     * ARCHIVED→ACTIVE (revive). Mọi chuyển về DRAFT đều bị từ chối
     * ({@code PLAN_STATE_TRANSITION_DENIED}). Cùng trạng thái → no-op 200.
     */
    ApiResponse<PlanResponse> changePlanStatus(UUID id, PlanStatus status);

    ApiResponse<Void> deletePlan(UUID id);

    /**
     * Gán lại tierOrder = 0,1,2... cho cac goi ACTIVE theo thứ tự D4:
     * isDefaultPlan -> contactSales -> price ASC NULLS LAST -> createdAt ASC.
     * Không trả ApiResponse vì còn được gọi nội bộ (create/update/delete) và từ DataInitializer.
     */
    void renumberTierOrder();

    /** Endpoint admin: renumber tierOrder rồi trả toàn bộ gói (để FE vẽ bảng tier mới). */
    ApiResponse<List<PlanResponse>> reorderPlans();
}