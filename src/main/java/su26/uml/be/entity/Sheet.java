package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sheets")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Sheet extends BaseEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "order_index")
    Integer orderIndex;

    @Column(name = "diagram_data", columnDefinition = "TEXT")
    String diagramData;

    // Nguồn chuẩn cho loại sơ đồ (usecase/class/sequence/...); backfill từ JSON diagramData,
    // fallback "activity" khi thiếu — FE không phải parse diagramData để lấy type nữa
    @Column(name = "diagram_type", length = 30)
    String diagramType;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Project project;
}
