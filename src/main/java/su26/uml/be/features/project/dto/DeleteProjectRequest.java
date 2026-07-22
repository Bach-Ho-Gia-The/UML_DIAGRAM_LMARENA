package su26.uml.be.features.project.dto;

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
public class DeleteProjectRequest {
    @NotEmpty(message = "IDS_REQUIRED")
    List<UUID> ids;
}