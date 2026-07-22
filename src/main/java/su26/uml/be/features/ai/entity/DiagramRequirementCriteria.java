package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "diagram_requirement_criteria")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiagramRequirementCriteria extends BaseEntity {

    @Column(name = "criteria_name", nullable = false)
    String criteriaName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "is_required")
    Boolean isRequired;

    @ManyToOne
    @JoinColumn(name = "diagram_type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramType diagramType;
}