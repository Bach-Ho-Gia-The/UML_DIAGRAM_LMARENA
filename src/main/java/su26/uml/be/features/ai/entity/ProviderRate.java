package su26.uml.be.features.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "provider_rates")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProviderRate {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    UUID id;

    @Column(nullable = false)
    String provider;

    @Column(name = "model_name", nullable = false)
    String modelName;

    @Column(name = "rate_in_per_1k", nullable = false, precision = 10, scale = 8)
    BigDecimal rateInPer1k;

    @Column(name = "rate_out_per_1k", nullable = false, precision = 10, scale = 8)
    BigDecimal rateOutPer1k;

    @Column(nullable = false)
    @Builder.Default
    String currency = "USD";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    boolean isActive = true;
}