package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import su26.uml.be.features.user.entity.User;

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