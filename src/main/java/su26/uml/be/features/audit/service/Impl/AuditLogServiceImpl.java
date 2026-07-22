package su26.uml.be.features.audit.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.audit.dto.AuditLogResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.audit.entity.AuditLog;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.audit.mapper.AuditLogMapper;
import su26.uml.be.features.audit.repository.AuditLogRepository;
import su26.uml.be.features.audit.repository.AuditLogSpecifications;
import su26.uml.be.features.audit.service.AuditLogService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    AuditLogRepository auditLogRepository;
    AuditLogMapper auditLogMapper;

    // Giờ nghiệp vụ dùng để quy đổi from/to (ngày) → mốc Instant. created_at trả về vẫn là UTC.
    @NonFinal
    @Value("${APP_TIMEZONE:UTC}")
    String appTimezone;

    @Override
    public ApiResponse<PagedResponse<AuditLogResponse>> getAuditLogs(
            String action, String actorId, LocalDate from, LocalDate to, int page, int size) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        UUID actorUuid = parseActorId(actorId);

        ZoneId zone = ZoneId.of(appTimezone);
        Instant fromInclusive = from == null ? null : from.atStartOfDay(zone).toInstant();
        // Bao trọn cả ngày 'to': dùng mốc đầu ngày kế tiếp làm cận trên (loại trừ).
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<AuditLog> specification =
                AuditLogSpecifications.withFilters(action, actorUuid, fromInclusive, toExclusive);

        Page<AuditLog> result = auditLogRepository.findAll(specification, pageable);

        List<AuditLogResponse> content = auditLogMapper.toResponseList(result.getContent());
        PagedResponse<AuditLogResponse> paged = PagedResponse.<AuditLogResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ApiResponse.success("Lấy nhật ký thao tác thành công", paged);
    }

    @Override
    public ApiResponse<List<String>> getDistinctActions() {
        return ApiResponse.success("Lấy danh sách action thành công",
                auditLogRepository.findDistinctActions());
    }

    /** actorId phải là UUID hợp lệ nếu có; sai định dạng → 1000 INVALID_REQUEST. */
    private UUID parseActorId(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(actorId.trim());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}