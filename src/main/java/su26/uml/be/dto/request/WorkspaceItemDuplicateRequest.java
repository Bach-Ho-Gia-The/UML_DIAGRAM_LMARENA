package su26.uml.be.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItemDuplicateRequest {

    // Folder đích chứa bản sao; null = gốc project
    UUID parentId;

    // Tên bản sao; null = tự sinh "Tên (2)", "Tên (3)"...
    @Size(max = 255, message = "WORKSPACE_NAME_TOO_LONG")
    String name;
}
