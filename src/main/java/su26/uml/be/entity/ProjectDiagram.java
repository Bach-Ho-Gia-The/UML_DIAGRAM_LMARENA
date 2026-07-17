package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_diagrams")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectDiagram extends BaseEntity {

    @Column(name = "title")
    String title;

    @Column(name = "diagram_data", columnDefinition = "TEXT")
    String diagramData;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Project project;

    @ManyToOne
    @JoinColumn(name = "diagram_type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramType diagramType;
}
