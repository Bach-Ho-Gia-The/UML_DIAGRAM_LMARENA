package su26.uml.be.service;

import su26.uml.be.dto.request.PlanRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.PlanResponse;

import java.util.List;
import java.util.UUID;

public interface PlanService {
    ApiResponse<List<PlanResponse>> getPublicPlans();
    ApiResponse<List<PlanResponse>> getAllPlans();
    ApiResponse<PlanResponse> getPlan(UUID id);
    ApiResponse<PlanResponse> createPlan(PlanRequest request);
    ApiResponse<PlanResponse> updatePlan(UUID id, PlanRequest request);
    ApiResponse<Void> deletePlan(UUID id);

    /** Gán lại tierOrder = 0,1,2... theo giá tăng dần cho các gói ACTIVE. */
    ApiResponse<List<PlanResponse>> reorderPlans();
}
