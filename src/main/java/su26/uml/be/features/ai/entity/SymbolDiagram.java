package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "symbol_diagrams")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SymbolDiagram extends BaseEntity {

    @Column(name = "symbol_name", nullable = false)
    String symbolName;

    @Column(name = "symbol_description", columnDefinition = "TEXT")
    String symbolDescription;

    @Column(name = "notation")
    String notation;

    @ManyToOne
    @JoinColumn(name = "diagram_type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramType diagramType;
}