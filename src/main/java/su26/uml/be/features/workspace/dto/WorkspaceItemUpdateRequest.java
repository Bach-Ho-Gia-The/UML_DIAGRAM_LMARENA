package su26.uml.be.features.workspace.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItemUpdateRequest {

    // Chỉ field nào gửi lên mới được cập nhật (partial update)
    @Size(max = 255, message = "WORKSPACE_NAME_TOO_LONG")
    String name;

    // Chỉ hợp lệ với item MARKDOWN
    String content;

    // Optimistic concurrency — optional: null = bỏ qua check (FE hiện tại chưa gửi)
    Long expectedVersion;
}