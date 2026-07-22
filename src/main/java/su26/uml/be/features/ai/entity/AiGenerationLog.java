package su26.uml.be.features.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;
import su26.uml.be.common.constant.enums.EstimationMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import su26.uml.be.features.user.entity.User;

@Entity
@Table(name = "ai_generation_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiGenerationLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    UUID id;

    @Column(name = "session_id")
    String sessionId;

    @Column(name = "user_id")
    String userId;

    @Column(name = "model_name", nullable = false)
    String modelName;

    @Column(name = "provider")
    String provider;

    @Column(name = "input_tokens")
    int inputTokens;

    @Column(name = "output_tokens")
    int outputTokens;

    @Column(name = "total_tokens")
    int totalTokens;

    @Column(name = "cost_usd", nullable = false, precision = 18, scale = 12)
    BigDecimal costUsd;

    @Enumerated(EnumType.STRING)
    @Column(name = "estimation_method", nullable = false)
    EstimationMethod estimationMethod;

    @Column(name = "latency_ms")
    long latencyMs;

    @Column(nullable = false)
    boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}