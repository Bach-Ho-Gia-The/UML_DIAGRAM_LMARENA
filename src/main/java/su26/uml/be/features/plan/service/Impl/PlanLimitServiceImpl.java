package su26.uml.be.features.plan.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.user.repository.UserRepository;
import su26.uml.be.features.plan.service.PlanLimitService;
import su26.uml.be.features.plan.service.PlanResolutionService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlanLimitServiceImpl implements PlanLimitService {

    PlanResolutionService planResolutionService;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertCanCreate(UUID userId, PlanFeatureKey key, long currentCount) {
        if (isAdmin(userId)) {
            return; // Admin: không gắn gói, capacity luôn unlimited.
        }
        int limit = limitOf(planResolutionService.resolveEntitlements(userId), key);
        if (limit == -1) {
            return; // unlimited
        }
        // limit 0 (chưa đặt / không có gói) → chặn ngay.
        if (currentCount >= limit) {
            throw new AppException(ErrorCode.PLAN_LIMIT_EXCEEDED);
        }
    }

    /** Admin không gắn gói, luôn được hạn mức cao nhất (unlimited) — nhận diện bằng role. */
    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().getRoleName()))
                .orElse(false);
    }

    /**
     * Giá trị limit của key trong entitlement HIỆU LỰC (snapshot của sub nếu có, ngược lại live
     * plan). null (chưa đặt) / không có gói → 0 (chặn). -1 = unlimited.
     */
    private int limitOf(PlanResolutionService.Entitlements e, PlanFeatureKey key) {
        if (e == null) {
            return 0;
        }
        Integer v = switch (key) {
            case MAX_PROJECTS -> e.maxProjects();
            case MAX_DIAGRAMS -> e.maxDiagrams();
            case AI_QUERIES -> e.aiQueries();
            case EXPORT_PDF -> e.maxExportPdf();
            case MAX_COLLABORATORS -> e.maxCollaborators();
        };
        return v != null ? v : 0;
    }
}