package su26.uml.be.features.audit.service;

import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.audit.dto.AuditLogResponse;
import su26.uml.be.common.response.PagedResponse;

import java.time.LocalDate;
import java.util.List;

public interface AuditLogService {

    /**
     * Truy vấn nhật ký admin có lọc động + phân trang (createdAt DESC).
     *
     * @param action  lọc theo action (optional)
     * @param actorId UUID admin dạng chuỗi (optional)
     * @param from    ngày bắt đầu yyyy-MM-dd, diễn giải theo giờ ứng dụng +7 (optional)
     * @param to      ngày kết thúc yyyy-MM-dd, bao trọn cả ngày (optional)
     * @param page    trang (0-based)
     * @param size    kích thước trang
     */
    ApiResponse<PagedResponse<AuditLogResponse>> getAuditLogs(
            String action, String actorId, LocalDate from, LocalDate to, int page, int size);

    /** Danh sách action đang tồn tại trong DB — để FE đổ dropdown lọc động. */
    ApiResponse<List<String>> getDistinctActions();
}