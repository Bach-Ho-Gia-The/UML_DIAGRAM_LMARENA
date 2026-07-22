package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "clarification_rounds")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClarificationRound extends BaseEntity {

    @Column(name = "round_number", nullable = false)
    Integer roundNumber;

    @Column(name = "status")
    String status;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    AnalysisSession session;

    @ManyToOne
    @JoinColumn(name = "triggered_by_intent_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Intent triggeredByIntent;
}