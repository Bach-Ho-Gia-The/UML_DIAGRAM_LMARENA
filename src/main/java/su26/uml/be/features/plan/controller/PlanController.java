package su26.uml.be.features.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.features.plan.dto.PlanRequest;
import su26.uml.be.features.plan.dto.PlanStatusRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.plan.dto.PlanResponse;
import su26.uml.be.features.plan.service.PlanService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Plans", description = "Subscription plan catalog (public pricing) + admin plan management.")
public class PlanController {

    PlanService planService;

    // ---------- PUBLIC ----------

    @GetMapping("/plans")
    @SecurityRequirements({})
    @Operation(summary = "Public plan catalog",
            description = "Returns active plans for the public pricing page (status = active), sorted by price.")
    public ApiResponse<List<PlanResponse>> getPublicPlans() {
        return planService.getPublicPlans();
    }

    // ---------- ADMIN ----------

    @GetMapping("/admin/plans")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all plans (admin)",
            description = "Returns every plan (all statuses) with live active-subscriber count.")
    public ApiResponse<List<PlanResponse>> getAllPlans() {
        return planService.getAllPlans();
    }

    @GetMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get plan detail (admin)")
    public ApiResponse<PlanResponse> getPlan(@PathVariable UUID id) {
        return planService.getPlan(id);
    }

    @PostMapping("/admin/plans")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create plan (admin)")
    public ApiResponse<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return planService.createPlan(request);
    }

    @PutMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update plan (admin)",
            description = "Partial update: chi cac field duoc gui moi bi thay doi (name/price/status... bo trong = giu nguyen). "
                    + "limits/features are replaced only when included in the body.")
    public ApiResponse<PlanResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return planService.updatePlan(id, request);
    }

    @PutMapping("/admin/plans/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change plan status (admin)",
            description = "Đổi trạng thái gói — endpoint RIÊNG, status không còn nằm trong body create/update. "
                    + "Chuyển hợp lệ: DRAFT→ACTIVE (bán), DRAFT→ARCHIVED, ACTIVE→ARCHIVED (ngừng bán — user "
                    + "đang dùng vẫn giữ quyền tới hết kỳ), ARCHIVED→ACTIVE (bán lại). "
                    + "Mọi chuyển về DRAFT bị từ chối (PLAN_STATE_TRANSITION_DENIED). "
                    + "Gói default ACTIVE cuối cùng không được rời ACTIVE (LAST_DEFAULT_PLAN_DENIED).")
    public ApiResponse<PlanResponse> changePlanStatus(@PathVariable UUID id,
            @Valid @RequestBody PlanStatusRequest request) {
        return planService.changePlanStatus(id, request.getStatus());
    }

    @DeleteMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete plan (admin)",
            description = "Hard-deletes a plan. Rejected if the plan has ANY subscription history "
                    + "(use ARCHIVED status instead) or if it is the last ACTIVE default plan "
                    + "(LAST_DEFAULT_PLAN_DENIED).")
    public ApiResponse<Void> deletePlan(@PathVariable UUID id) {
        return planService.deletePlan(id);
    }

    @PostMapping("/admin/plans/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Renumber tierOrder (admin)",
            description = "Gán lại tierOrder = 0,1,2... cho các gói ACTIVE theo thứ tự: gói mặc định (isDefaultPlan) "
                    + "-> gói báo giá (contactSales) -> giá tăng dần (NULLS LAST) -> ngày tạo. "
                    + "tierOrder do hệ thống tự tính, admin không nhập tay.")
    public ApiResponse<List<PlanResponse>> reorderPlans() {
        return planService.reorderPlans();
    }
}