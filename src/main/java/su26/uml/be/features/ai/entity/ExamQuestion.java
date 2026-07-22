package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exam_questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamQuestion extends BaseEntity {

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    String questionText;

    @Column(name = "question_order")
    Integer questionOrder;

    @Column(name = "question_type")
    String questionType;

    @ManyToOne
    @JoinColumn(name = "exam_upload_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ExamUpload examUpload;
}