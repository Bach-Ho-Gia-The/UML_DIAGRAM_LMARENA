package su26.uml.be.features.workspace.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import su26.uml.be.common.constant.enums.WorkspaceItemKind;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItemResponse {
    UUID id;
    UUID projectId;
    UUID parentId;
    String name;
    WorkspaceItemKind kind;
    Integer orderIndex;
    UUID sheetId;
    String diagramType;
    String content;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}