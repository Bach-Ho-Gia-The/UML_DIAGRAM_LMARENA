package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "intents")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Intent extends BaseEntity {

    @Column(name = "intent_name", nullable = false)
    String intentName;

    @Column(name = "confidence_score")
    BigDecimal confidenceScore;

    @ManyToOne
    @JoinColumn(name = "user_message_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UserMessage userMessage;
}
