package su26.uml.be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import su26.uml.be.enums.DiagramVersionSource;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// NON_NULL vì response danh sách bỏ diagramData (nặng) — chỉ GET chi tiết/restore mới trả
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagramVersionResponse {
    UUID id;
    UUID sheetId;
    Long versionNumber;
    String name;
    String note;
    DiagramVersionSource source;
    String diagramData;
    String contentHash;
    Integer schemaVersion;
    UUID restoredFromVersionId;
    LocalDateTime createdAt;
}
