package su26.uml.be.features.ai.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiSourceDocument {

    String title;

    String url;

    BigDecimal score;

    String snippet;
}