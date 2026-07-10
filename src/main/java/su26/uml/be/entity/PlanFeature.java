package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import su26.uml.be.enums.PlanFeatureKey;

@Entity
@Table(name = "plan_features",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "feature_key"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanFeature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_key", nullable = false, length = 30)
    PlanFeatureKey featureKey;

    @Column(name = "limit_value", nullable = false)
    @Builder.Default
    Integer limitValue = 0; // -1 = unlimited
}
