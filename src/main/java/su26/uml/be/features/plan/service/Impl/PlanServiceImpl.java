package su26.uml.be.features.plan.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.plan.dto.PlanRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.plan.dto.PlanResponse;
import su26.uml.be.features.plan.entity.FeatureCatalog;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.features.plan.entity.PlanFeature;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.constant.enums.PlanStatus;
import su26.uml.be.common.constant.enums.SubscriptionStatus;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.plan.mapper.PlanMapper;
import su26.uml.be.features.plan.repository.FeatureCatalogRepository;
import su26.uml.be.features.plan.repository.PlanRepository;
import su26.uml.be.features.subscription.repository.SubscriptionRepository;
import su26.uml.be.features.plan.service.PlanService;

import java.math.BigDecimal;
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
        applyDefaultsForCreate(request); // CHỈ create — update KHÔNG gọi (C8: không hạ gói ACTIVE về DRAFT)
        Plan plan = planMapper.toPlan(request);
        normalizePrice(plan, request);
        applyLimits(plan, request);
        if (request.getEnabledFeatureIds() != null) {
            plan.setEnabledFeatureIds(new HashSet<>(request.getEnabledFeatureIds()));
        }

        Plan saved = planRepository.save(plan);
        renumberTierOrder();
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
        // D2: contactSales=true ⇒ price = null (mapper không tự clear được khi price không được gửi).
        normalizePrice(plan, request);

        // Limits & enabled features are replaced only when explicitly provided.
        if (request.getLimits() != null) {
            applyLimits(plan, request);
        }
        if (request.getEnabledFeatureIds() != null) {
            plan.setEnabledFeatureIds(new HashSet<>(request.getEnabledFeatureIds()));
        }

        Plan saved = planRepository.save(plan);
        renumberTierOrder();
        return ApiResponse.success("Cập nhật gói thành công",
                buildResponse(planRepository.findById(saved.getId()).orElse(saved), loadCatalog()));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deletePlan(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // D7: không xoá gói mặc định ACTIVE cuối cùng — hệ thống phải luôn có 1 default plan.
        if (isActiveDefault(plan)
                && planRepository.countByStatusAndIsDefaultPlanTrueAndIdNot(PlanStatus.ACTIVE, plan.getId()) == 0) {
            throw new AppException(ErrorCode.LAST_DEFAULT_PLAN_DENIED);
        }

        // D8: không xoá gói còn subscriber ACTIVE.
        if (subscriptionRepository.existsByPlanAndStatus(plan, SubscriptionStatus.ACTIVE)) {
            throw new AppException(ErrorCode.PLAN_HAS_SUBSCRIBERS);
        }

        planRepository.delete(plan);
        renumberTierOrder();
        return ApiResponse.success("Xoá gói thành công", null);
    }

    @Override
    @Transactional
    public ApiResponse<List<PlanResponse>> reorderPlans() {
        renumberTierOrder();
        return getAllPlans();
    }

    @Override
    @Transactional
    public void renumberTierOrder() {
        List<Plan> active = planRepository.findAllOrderedByTier(PlanStatus.ACTIVE);
        int order = 0;
        for (Plan p : active) {
            if (!Integer.valueOf(order).equals(p.getTierOrder())) {
                p.setTierOrder(order);
            }
            order++;
        }
        planRepository.saveAll(active);
    }

    // --- helpers ---

    /**
     * Validate create/update gói:
     * <ul>
     *   <li>D2 — contactSales=true ⇒ price phải null/0 (hệ thống ép null); contactSales=false ⇒ price bắt buộc và &gt;= 0</li>
     *   <li>D7 — chỉ 1 gói mặc định ACTIVE; không được bỏ mặc định / hạ trạng thái của gói mặc định ACTIVE cuối cùng</li>
     *   <li>D8 — không archive gói còn subscriber ACTIVE</li>
     * </ul>
     */
    private void validatePlanRequest(PlanRequest request, Plan existing) {
        // ── D2: price ──
        boolean contactSales = effectiveContactSales(request, existing);
        BigDecimal price = request.getPrice() != null ? request.getPrice()
                : (existing != null ? existing.getPrice() : null);
        if (contactSales) {
            // Gói báo giá: giá dương là sai (admin phải clear price trước). price = null/0 → ép null ở normalizePrice.
            if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
                throw new AppException(ErrorCode.ENTERPRISE_PRICE_MUST_BE_NULL);
            }
        } else {
            if (price == null) {
                throw new AppException(ErrorCode.SELF_SERVE_PRICE_REQUIRED);
            }
            if (price.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.PLAN_PRICE_INVALID);
            }
        }

        // ── D7: default plan ──
        boolean isDefault = effectiveIsDefault(request, existing);
        PlanStatus status = effectiveStatus(request, existing);
        long otherActiveDefaults = existing != null
                ? planRepository.countByStatusAndIsDefaultPlanTrueAndIdNot(PlanStatus.ACTIVE, existing.getId())
                : (planRepository.existsByStatusAndIsDefaultPlanTrue(PlanStatus.ACTIVE) ? 1L : 0L);

        if (isDefault && status == PlanStatus.ACTIVE && otherActiveDefaults > 0) {
            throw new AppException(ErrorCode.DEFAULT_PLAN_ALREADY_EXISTS);
        }
        // Gói đang là default: nếu là default ACTIVE cuối cùng thì không được bỏ cờ / rời ACTIVE.
        if (existing != null && Boolean.TRUE.equals(existing.getIsDefaultPlan())
                && otherActiveDefaults == 0
                && !(isDefault && status == PlanStatus.ACTIVE)) {
            throw new AppException(ErrorCode.LAST_DEFAULT_PLAN_DENIED);
        }

        // ── D8: archive gói có subscriber ──
        if (existing != null
                && status == PlanStatus.ARCHIVED
                && existing.getStatus() != PlanStatus.ARCHIVED
                && subscriptionRepository.existsByPlanAndStatus(existing, SubscriptionStatus.ACTIVE)) {
            throw new AppException(ErrorCode.PLAN_HAS_SUBSCRIBERS);
        }
    }

    private boolean isActiveDefault(Plan plan) {
        return Boolean.TRUE.equals(plan.getIsDefaultPlan()) && plan.getStatus() == PlanStatus.ACTIVE;
    }

    private boolean effectiveContactSales(PlanRequest request, Plan existing) {
        if (request.getContactSales() != null) {
            return request.getContactSales();
        }
        return existing != null && existing.isContactSales();
    }

    private boolean effectiveIsDefault(PlanRequest request, Plan existing) {
        if (request.getIsDefaultPlan() != null) {
            return request.getIsDefaultPlan();
        }
        return existing != null && Boolean.TRUE.equals(existing.getIsDefaultPlan());
    }

    private PlanStatus effectiveStatus(PlanRequest request, Plan existing) {
        if (request.getStatus() != null) {
            return request.getStatus();
        }
        if (existing != null && existing.getStatus() != null) {
            return existing.getStatus();
        }
        return PlanStatus.DRAFT;
    }

    /** D2: gói báo giá không được có giá — ép price = null sau khi mapper đã map. */
    private void normalizePrice(Plan plan, PlanRequest request) {
        boolean contactSales = request.getContactSales() != null
                ? request.getContactSales()
                : plan.isContactSales();
        if (contactSales) {
            plan.setPrice(null);
        }
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

    /**
     * Defaults CHỈ áp dụng khi tạo gói. Update tuyệt đối không gọi: set status=DRAFT sẽ hạ
     * gói ACTIVE về DRAFT nếu admin không gửi status (lỗi C8 trong planning).
     */
    private void applyDefaultsForCreate(PlanRequest request) {
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
        // is_default_plan NOT NULL — chuẩn hoá null → false LÚC TẠO (không phải normalize false ở update).
        if (request.getIsDefaultPlan() == null) {
            request.setIsDefaultPlan(false);
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
