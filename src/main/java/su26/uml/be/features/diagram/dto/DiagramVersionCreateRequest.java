package su26.uml.be.features.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import su26.uml.be.common.constant.enums.DiagramVersionSource;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiagramVersionCreateRequest {

    // null/blank = server tự sinh "Version {n}"
    @Size(max = 255, message = "DIAGRAM_VERSION_NAME_TOO_LONG")
    String name;

    String note;

    @NotNull(message = "DIAGRAM_VERSION_SOURCE_REQUIRED")
    DiagramVersionSource source;

    // Snapshot JSON đầy đủ {schemaVersion, diagramType, nodes, edges, viewport?}
    @NotBlank(message = "DIAGRAM_VERSION_DATA_REQUIRED")
    String diagramData;

    // false/null: nội dung trùng hash với version đã có → trả về version cũ, không tạo mới.
    // true (MANUAL/checkpoint chủ đích): luôn tạo bản ghi mới.
    Boolean force;
}