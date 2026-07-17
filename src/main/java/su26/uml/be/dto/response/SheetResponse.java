package su26.uml.be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SheetResponse {
    UUID id;
    String name;
    Integer orderIndex;
    String diagramData;
    String diagramType;
    UUID projectId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
