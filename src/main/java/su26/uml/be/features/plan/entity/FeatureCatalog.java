package su26.uml.be.features.plan.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Admin-managed master catalog of comparable features (rows of the pricing comparison matrix).
 * Each plan enables a subset of these via {@code Plan.enabledFeatureIds}.
 */
@Entity
@Table(name = "feature_catalog")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeatureCatalog extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    String label;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;
}