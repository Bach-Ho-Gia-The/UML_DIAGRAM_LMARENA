package su26.uml.be.features.workspace.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItemDeleteRequest {

    @NotEmpty(message = "WORKSPACE_IDS_REQUIRED")
    List<UUID> ids;

    // false/null: folder còn nội dung sẽ bị chặn (WORKSPACE_FOLDER_NOT_EMPTY);
    // true: xóa đệ quy toàn bộ subtree (atomic trong 1 transaction)
    Boolean recursive;
}