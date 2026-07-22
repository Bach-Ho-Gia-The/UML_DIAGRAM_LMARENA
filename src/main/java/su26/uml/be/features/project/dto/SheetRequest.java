package su26.uml.be.features.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SheetRequest {
    String name;

    Integer orderIndex;

    String diagramData;

    UUID projectId;
}