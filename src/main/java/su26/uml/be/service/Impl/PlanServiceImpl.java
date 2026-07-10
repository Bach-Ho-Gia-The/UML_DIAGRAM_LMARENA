package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.dto.request.PlanRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.PlanResponse;
import su26.uml.be.entity.FeatureCatalog;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.PlanStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.mapper.PlanMapper;
import su26.uml.be.repository.FeatureCatalogRepository;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.service.PlanService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlanServiceImpl implements PlanService {

    PlanRepository planRepository;
    SubscriptionRepository subscriptionRepository;
    FeatureCatalogRepository featureCatalogRepository;
    PlanMapper planMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PlanResponse>> getPublicPlans() {
        List<FeatureCatalog> catalog = loadCatalog();
        List<PlanResponse> result = planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE).stream()
                .map(plan -> buildResponse(plan, catalog))
                .toList();
        return ApiResponse.success("OK", result);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PlanResponse>> getAllPlans() {
        List<FeatureCatalog> catalog = loadCatalog();
        List<PlanResponse> result = planRepository.findAll().stream()
                .map(plan -> buildResponse(plan, catalog))
                .toList();
        return ApiResponse.success("OK", result);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PlanResponse> getPlan(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));
        return ApiResponse.success("OK", buildResponse(plan, loadCatalog()));
    }

    @Override
    @Transactional
    public ApiResponse<PlanResponse> createPlan(PlanRequest request) {
        if (planRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AppException(ErrorCode.PLAN_NAME_EXISTED);
        }

        applyDefaults(request);
        Plan plan = planMapper.toPlan(request);
        applyLimits(plan, request);
        if (request.getEnabledFeatureIds() != null) {
            plan.setEnabledFeatureIds(new HashSet<>(request.getEnabledFeatureIds()));
        }

        Plan saved = planRepository.save(plan);
        return ApiResponse.success("Tạo gói thành công", buildResponse(saved, loadCatalog()));
    }

    @Override
    @Transactional
    public ApiResponse<PlanResponse> updatePlan(UUID id, PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        if (request.getName() != null
                && !request.getName().equalsIgnoreCase(plan.getName())
                && planRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AppException(ErrorCode.PLAN_NAME_EXISTED);
        }

        // Partial update of scalar fields (nulls ignored by the mapper).
        planMapper.updatePlan(request, plan);

        // Limits & enabled features are replaced only when explicitly provided.
        if (request.getLimits() != null) {
            applyLimits(plan, request);
        }
        if (request.getEnabledFeatureIds() != null) {
            plan.setEnabledFeatureIds(new HashSet<>(request.getEnabledFeatureIds()));
        }

        Plan saved = planRepository.save(plan);
        return ApiResponse.success("Cập nhật gói thành công", buildResponse(saved, loadCatalog()));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deletePlan(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        if (subscriptionRepository.existsByPlanAndStatus(plan, SubscriptionStatus.ACTIVE)) {
            throw new AppException(ErrorCode.PLAN_HAS_SUBSCRIBERS);
        }

        planRepository.delete(plan);
        return ApiResponse.<Void>builder().build();
    }

    // --- helpers ---

    private List<FeatureCatalog> loadCatalog() {
        return featureCatalogRepository.findAllByOrderBySortOrderAscLabelAsc();
    }

    /**
     * Builds a plan response including the full comparison matrix: every catalog feature is emitted
     * with an {@code included} flag for this plan. Rows are identical/ordered across all plans, so
     * the FE can render aligned columns directly.
     */
    private PlanResponse buildResponse(Plan plan, List<FeatureCatalog> catalog) {
        long subscribers = subscriptionRepository.countByPlanAndStatus(plan, SubscriptionStatus.ACTIVE);
        Set<UUID> enabled = plan.getEnabledFeatureIds();
        List<PlanResponse.FeatureCell> cells = catalog.stream()
                .map(feature -> PlanResponse.FeatureCell.builder()
                        .id(feature.getId().toString())
                        .label(feature.getLabel())
                        .included(enabled != null && enabled.contains(feature.getId()))
                        .build())
                .toList();
        return planMapper.toPlanResponse(plan, subscribers, cells);
    }

    private void applyDefaults(PlanRequest request) {
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            request.setCurrency("VND");
        }
        if (request.getStatus() == null) {
            request.setStatus(PlanStatus.DRAFT);
        }
        if (request.getPopular() == null) {
            request.setPopular(false);
        }
        if (request.getYearlyBilling() == null) {
            request.setYearlyBilling(false);
        }
        if (request.getYearlyDiscount() == null) {
            request.setYearlyDiscount(0);
        }
        if (request.getContactSales() == null) {
            request.setContactSales(false);
        }
    }

    /**
     * Rebuilds the plan_features rows from the request's limits object.
     * No grandfathering: existing subscribers immediately see the new limits.
     */
    private void applyLimits(Plan plan, PlanRequest request) {
        PlanRequest.PlanLimitsRequest limits = request.getLimits();
        plan.getPlanFeatures().clear();
        if (limits == null) {
            return;
        }
        addFeature(plan, PlanFeatureKey.MAX_PROJECTS, limits.getProjects());
        addFeature(plan, PlanFeatureKey.MAX_DIAGRAMS, limits.getDiagrams());
        addFeature(plan, PlanFeatureKey.AI_QUERIES, limits.getAiQueries());
        addFeature(plan, PlanFeatureKey.MAX_COLLABORATORS, limits.getCollaborators());
    }

    private void addFeature(Plan plan, PlanFeatureKey key, Integer value) {
        if (value == null) {
            return;
        }
        plan.getPlanFeatures().add(PlanFeature.builder()
                .plan(plan)
                .featureKey(key)
                .limitValue(value)
                .build());
    }
}
