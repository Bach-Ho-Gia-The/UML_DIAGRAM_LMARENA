package su26.uml.be.dto.response;

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
@Schema(name = "TopProjectResponse", description = "A project ranked by diagram (sheet) count.")
public class TopProjectResponse {

    @Schema(description = "Project id (UUID).", example = "a1b2c3d4-...")
    String projectId;

    @Schema(description = "Project name.", example = "E-commerce UML")
    String projectName;

    @Schema(description = "Owner email.", example = "owner@example.com")
    String ownerEmail;

    @Schema(description = "Number of diagrams (sheets) in this project.", example = "8")
    long diagramCount;
}
