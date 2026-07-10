package su26.uml.be.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(name = "FeatureCatalogRequest", description = "Create/update payload for a catalog feature.")
public class FeatureCatalogRequest {

    @NotBlank(message = "FEATURE_LABEL_REQUIRED")
    @Schema(example = "Vẽ diagram")
    String label;

    @Schema(description = "Display order in the comparison matrix (asc).", example = "1")
    Integer sortOrder;
}
