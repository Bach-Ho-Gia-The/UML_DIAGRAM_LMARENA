package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "ai_solutions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiSolution extends BaseEntity {

    @Column(name = "solution_content", columnDefinition = "TEXT")
    String solutionContent;

    @Column(name = "model_name")
    String modelName;

    @Column(name = "confidence_score")
    BigDecimal confidenceScore;

    @ManyToOne
    @JoinColumn(name = "exam_upload_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ExamUpload examUpload;
}