package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.dto.request.PlanRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.PlanResponse;
import su26.uml.be.service.PlanService;

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
            description = "Full update: send the whole plan object (name & price required). "
                    + "limits/features are replaced only when included in the body.")
    public ApiResponse<PlanResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return planService.updatePlan(id, request);
    }

    @DeleteMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete plan (admin)",
            description = "Hard-deletes a plan. Rejected if it still has active subscribers (use archived status instead).")
    public ApiResponse<Void> deletePlan(@PathVariable UUID id) {
        return planService.deletePlan(id);
    }

    @PostMapping("/admin/plans/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reorder plans by price (admin)",
            description = "Gán lại tierOrder = 0,1,2... theo giá tăng dần cho các gói ACTIVE.")
    public ApiResponse<List<PlanResponse>> reorderPlans() {
        return planService.reorderPlans();
    }
}
