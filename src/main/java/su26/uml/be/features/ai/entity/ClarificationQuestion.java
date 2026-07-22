package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "clarification_questions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClarificationQuestion extends BaseEntity {

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    String answerText;

    @Column(name = "question_order")
    Integer questionOrder;

    @Column(name = "answered_at")
    LocalDateTime answeredAt;

    @ManyToOne
    @JoinColumn(name = "clarification_round_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ClarificationRound clarificationRound;

    @ManyToOne
    @JoinColumn(name = "diagram_type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramType diagramType;
}