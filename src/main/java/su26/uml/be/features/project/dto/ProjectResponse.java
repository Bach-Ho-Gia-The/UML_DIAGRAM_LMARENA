package su26.uml.be.features.project.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "ProjectResponse", description = "UML Project details.")
public class ProjectResponse {
    @Schema(description = "Unique project identifier.")
    UUID id;

    @Schema(description = "Project name.")
    String projectName;

    @Schema(description = "Project description.")
    String description;

    @Schema(description = "Owner ID.")
    UUID userId;

    @Schema(description = "Owner display name.")
    String ownerName;

    @Schema(description = "Owner email address.")
    String ownerEmail;

    @Schema(description = "Number of sheets (diagram tabs) in the project.")
    Integer diagramCount;

    @Schema(description = "List of sheets (tabs) in the project.")
    List<SheetResponse> sheets;

    @Schema(description = "Whether this project is a draft.")
    Boolean isDraft;

    @Schema(description = "Whether this project is archived.")
    Boolean isArchived;

    @Schema(description = "Whether this project is publicly accessible.")
    Boolean publicAccess;

    @Schema(description = "Creation timestamp.")
    LocalDateTime createdAt;

    @Schema(description = "Last update timestamp.")
    LocalDateTime updatedAt;
}