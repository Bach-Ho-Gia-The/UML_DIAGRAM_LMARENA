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

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
                .map(plan -> {
                    PlanResponse r = buildResponse(plan, catalog);
                    // Rate limit là thông số kỹ thuật — không lộ ra public.
                    r.setRateLimitPer10s(null);
                    r.setRateLimitPerMin(null);
                    return r;
                })
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

        validatePlanRequest(request, null);
        applyDefaults(request);
        Plan plan = planMapper.toPlan(request);
        applyLimits(plan, request);
        if (request.getEnabledFeatureIds() != null) {
            plan.setEnabledFeatureIds(new HashSet<>(request.getEnabledFeatureIds()));
        }

        Plan saved = planRepository.save(plan);
        renumberTierOrderByPrice();
        return ApiResponse.success("Tạo gói thành công",
                buildResponse(planRepository.findById(saved.getId()).orElse(saved), loadCatalog()));
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

        validatePlanRequest(request, plan);
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
        renumberTierOrderByPrice();
        return ApiResponse.success("Cập nhật gói thành công",
                buildResponse(planRepository.findById(saved.getId()).orElse(saved), loadCatalog()));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deletePlan(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        if (Boolean.TRUE.equals(plan.getIsBasePlan())) {
            throw new AppException(ErrorCode.PLAN_BASE_DELETE_DENIED);
        }

        if (subscriptionRepository.existsByPlanAndStatus(plan, SubscriptionStatus.ACTIVE)) {
            throw new AppException(ErrorCode.PLAN_HAS_SUBSCRIBERS);
        }

        planRepository.delete(plan);
        renumberTierOrderByPrice();
        return ApiResponse.<Void>builder().build();
    }

    @Override
    @Transactional
    public ApiResponse<List<PlanResponse>> reorderPlans() {
        renumberTierOrderByPrice();
        return getAllPlans();
    }

    // --- helpers ---

    private void validatePlanRequest(PlanRequest request, Plan existing) {
        // price >= 0 (null đã bị @NotNull chặn ở DTO, guard thêm để defense-in-depth)
        if (request.getPrice() == null || request.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.PLAN_PRICE_INVALID);
        }

        // isBasePlan: only one true in the system
        if (Boolean.TRUE.equals(request.getIsBasePlan())) {
            boolean alreadyHasBase = planRepository.findByIsBasePlanTrue()
                    .map(p -> existing == null || !p.getId().equals(existing.getId()))
                    .orElse(false);
            if (alreadyHasBase) {
                throw new AppException(ErrorCode.BASE_PLAN_ALREADY_EXISTS);
            }
        }

        // Giá unique (không cho 2 gói cùng giá) — nền cho auto tierOrder theo giá.
        boolean priceTaken = existing == null
                ? planRepository.existsByPrice(request.getPrice())
                : planRepository.existsByPriceAndIdNot(request.getPrice(), existing.getId());
        if (priceTaken) {
            throw new AppException(ErrorCode.PLAN_PRICE_DUPLICATE);
        }
        // tierOrder KHÔNG do admin nhập — BE tự tính theo giá (renumberTierOrderByPrice sau khi lưu).
    }

    /** Gán tierOrder = 0,1,2... theo giá tăng dần cho các gói ACTIVE (giá unique → không trùng). */
    private void renumberTierOrderByPrice() {
        List<Plan> active = planRepository.findByStatusOrderByPriceAsc(PlanStatus.ACTIVE);
        int order = 0;
        for (Plan p : active) {
            if (!Integer.valueOf(order).equals(p.getTierOrder())) {
                p.setTierOrder(order);
            }
            order++;
        }
        planRepository.saveAll(active);
    }

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
     * Reconciles the plan_features rows with the request's limits object via an in-place upsert:
     * existing keys are updated, dropped keys are orphan-removed, new keys are inserted. We must
     * NOT clear() then re-add the same (plan_id, feature_key) — Hibernate flushes the INSERT before
     * the orphan DELETE, which violates the unique constraint. No grandfathering: existing
     * subscribers immediately see the new limits.
     */
    private void applyLimits(Plan plan, PlanRequest request) {
        PlanRequest.PlanLimitsRequest limits = request.getLimits();
        Map<PlanFeatureKey, Integer> desired = new EnumMap<>(PlanFeatureKey.class);
        if (limits != null) {
            putLimit(desired, PlanFeatureKey.MAX_PROJECTS, limits.getProjects());
            putLimit(desired, PlanFeatureKey.MAX_DIAGRAMS, limits.getDiagrams());
            putLimit(desired, PlanFeatureKey.AI_QUERIES, limits.getAiQueries());
            putLimit(desired, PlanFeatureKey.EXPORT_PDF, limits.getExportPdf());
            putLimit(desired, PlanFeatureKey.MAX_COLLABORATORS, limits.getCollaborators());
        }

        // Update rows still wanted (in place) / remove rows no longer wanted.
        plan.getPlanFeatures().removeIf(pf -> {
            Integer value = desired.remove(pf.getFeatureKey());
            if (value == null) {
                return true; // key no longer set -> orphan-remove
            }
            pf.setLimitValue(value); // key still set -> update in place (no re-insert)
            return false;
        });

        // Insert only genuinely new keys.
        desired.forEach((key, value) -> plan.getPlanFeatures().add(PlanFeature.builder()
                .plan(plan)
                .featureKey(key)
                .limitValue(value)
                .build()));
    }

    private void putLimit(Map<PlanFeatureKey, Integer> map, PlanFeatureKey key, Integer value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
