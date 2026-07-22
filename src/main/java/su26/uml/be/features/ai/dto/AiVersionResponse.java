package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiVersionResponse {
    String version;
    String llmProvider;
    String model;
    String environment;
}