package su26.uml.be.features.audit.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import su26.uml.be.features.audit.dto.AuditLogResponse;
import su26.uml.be.features.audit.entity.AuditLog;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    // Dùng chung để parse cột detail (JSON text) → object cho response.
    ObjectMapper DETAIL_MAPPER = new ObjectMapper();

    @Mapping(target = "detail", source = "detail", qualifiedByName = "parseDetail")
    AuditLogResponse toResponse(AuditLog entity);

    List<AuditLogResponse> toResponseList(List<AuditLog> entities);

    /** Chuyển JSON text đã lưu thành object để Jackson trả về dạng lồng (không phải chuỗi). */
    @Named("parseDetail")
    default Object parseDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        try {
            return DETAIL_MAPPER.readValue(detail, Object.class);
        } catch (Exception e) {
            // Detail hỏng không được làm gãy phản hồi — trả nguyên chuỗi để không mất dữ liệu.
            return detail;
        }
    }
}