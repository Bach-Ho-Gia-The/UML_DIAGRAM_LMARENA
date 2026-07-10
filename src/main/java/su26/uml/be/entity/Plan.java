package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Plan extends BaseEntity {

    @Column(nullable = false)
    String name;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "max_diagrams")
    Integer maxDiagrams; // -1 for unlimited

    @Column(name = "duration_days")
    Integer durationDays;
}
