package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "diagram_types")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiagramType extends BaseEntity {

    @Column(name = "type_name", nullable = false, unique = true)
    String typeName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;
}