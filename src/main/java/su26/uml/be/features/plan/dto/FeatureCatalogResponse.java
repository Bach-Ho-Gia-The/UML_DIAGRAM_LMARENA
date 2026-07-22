package su26.uml.be.features.plan.dto;

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
@Schema(name = "FeatureCatalogResponse", description = "A catalog feature (one row of the pricing comparison matrix).")
public class FeatureCatalogResponse {
    String id;
    String label;
    Integer sortOrder;
}