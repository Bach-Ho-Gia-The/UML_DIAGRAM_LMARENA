package su26.uml.be.features.workspace.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItemMoveRequest {

    // Folder đích; null = chuyển ra gốc project
    UUID parentId;

    // Vị trí trong folder đích; null = thêm vào cuối. Order index anh em được normalize lại 0..n-1
    Integer orderIndex;

    // Optimistic concurrency — optional
    Long expectedVersion;
}