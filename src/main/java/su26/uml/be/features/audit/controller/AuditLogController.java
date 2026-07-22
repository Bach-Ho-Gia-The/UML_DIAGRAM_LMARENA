package su26.uml.be.features.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import su26.uml.be.common.config.SwaggerExamples;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.audit.dto.AuditLogResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.audit.service.AuditLogService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Audit Log", description = "Nhật ký thao tác quản trị (chỉ ADMIN).")
public class AuditLogController {

    AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Danh sách nhật ký thao tác quản trị",
            description = "Lọc động theo action, actorId (UUID), khoảng ngày [from, to] (yyyy-MM-dd, "
                    + "diễn giải theo giờ +7). Phân trang, sắp xếp createdAt DESC. "
                    + "Mặc định: page=0, size=20. created_at trả về theo UTC (hậu tố Z).\n\n"
                    + SwaggerExamples.AUDIT_ACTIONS_CATALOG)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Trang nhật ký (dòng hệ thống có actorName='UMLAdminSystem', actorId/actorEmail null).",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.AUDIT_LOGS_RESPONSE)))
    public ApiResponse<PagedResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditLogService.getAuditLogs(action, actorId, from, to, page, size);
    }

    @GetMapping("/actions")
    @Operation(summary = "Danh sách action đang tồn tại",
            description = "Trả về các action distinct trong DB để FE đổ dropdown lọc (không hardcode). "
                    + "Action mới (vd. SUBSCRIPTION_*) sẽ tự xuất hiện ở đây ngay khi có endpoint gắn @Auditable.\n\n"
                    + SwaggerExamples.AUDIT_ACTIONS_CATALOG)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Danh sách action distinct.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.AUDIT_ACTIONS_RESPONSE)))
    public ApiResponse<List<String>> getActions() {
        return auditLogService.getDistinctActions();
    }
}