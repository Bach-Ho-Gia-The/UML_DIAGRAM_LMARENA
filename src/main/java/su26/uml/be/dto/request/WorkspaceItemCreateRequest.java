package su26.uml.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import su26.uml.be.enums.WorkspaceItemKind;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItemCreateRequest {

    // null = tạo ở gốc project; nếu có thì phải là FOLDER cùng project
    UUID parentId;

    @NotBlank(message = "WORKSPACE_NAME_REQUIRED")
    @Size(max = 255, message = "WORKSPACE_NAME_TOO_LONG")
    String name;

    @NotNull(message = "WORKSPACE_KIND_REQUIRED")
    WorkspaceItemKind kind;

    // Chỉ cho kind = MARKDOWN (mặc định chuỗi rỗng)
    String content;

    // Chỉ cho kind = DIAGRAM: loại sơ đồ + dữ liệu canvas ban đầu (JSON string như SheetRequest)
    String diagramType;
    String diagramData;

    // Vị trí chèn trong folder cha; null = thêm vào cuối
    Integer orderIndex;
}
