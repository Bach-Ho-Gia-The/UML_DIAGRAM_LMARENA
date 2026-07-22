package su26.uml.be.features.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AuditLogResponse", description = "Một dòng nhật ký thao tác quản trị.")
public class AuditLogResponse {

    @Schema(description = "Khóa số tự tăng của bản ghi audit.", example = "1024")
    Long id;

    @Schema(description = "UUID của admin thực hiện.", example = "3f1e9c2a-8b7d-4a11-9f0e-2c6d5b4a3e21")
    UUID actorId;

    @Schema(description = "Họ tên admin thực hiện.", example = "Nguyễn Minh Anh")
    String actorName;

    @Schema(description = "Email admin thực hiện.", example = "admin@uml.studio")
    String actorEmail;

    @Schema(description = "Tên hành động.", example = "USER_LOCK")
    String action;

    @Schema(description = "Loại đối tượng bị tác động.", example = "USER")
    String targetType;

    @Schema(description = "Id đối tượng bị tác động (chuỗi).", example = "42")
    String targetId;

    @Schema(description = "Chi tiết dạng JSON (vd. {before, after}).",
            example = "{\"before\":\"ACTIVE\",\"after\":\"LOCKED\"}")
    Object detail;

    @Schema(description = "Thời điểm ghi nhận (UTC, ISO-8601 với hậu tố Z).",
            example = "2026-07-01T14:32:10Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    Instant createdAt;
}