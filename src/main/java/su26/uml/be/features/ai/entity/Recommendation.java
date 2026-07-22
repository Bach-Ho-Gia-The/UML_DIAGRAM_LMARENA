package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "recommendations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Recommendation extends BaseEntity {

    @Column(name = "recommendation_content", columnDefinition = "TEXT")
    String recommendationContent;

    @Column(name = "priority")
    String priority;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    AnalysisSession session;

    @ManyToOne
    @JoinColumn(name = "diagram_type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramType diagramType;

    @ManyToOne
    @JoinColumn(name = "criteria_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramRequirementCriteria criteria;
}