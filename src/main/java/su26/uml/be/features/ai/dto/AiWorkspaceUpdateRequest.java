package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiWorkspaceUpdateRequest {
    String model;
    String chatProvider;
    String chatMode;
    Double temperature;
    Integer topN;
    Double similarityThreshold;
    Integer openAiHistory;
    String openAiPrompt;
    String queryRefusalResponse;
}