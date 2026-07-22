package su26.uml.be.features.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectRequest {
    @NotBlank(message = "PROJECT_NAME_REQUIRED")
    @Size(max = 255, message = "PROJECT_NAME_TOO_LONG")
    String projectName;

    @Size(max = 1000, message = "DESCRIPTION_TOO_LONG")
    String description;

    Boolean isDraft;

    Boolean publicAccess;
}