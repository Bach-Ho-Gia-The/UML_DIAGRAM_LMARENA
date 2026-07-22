package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiWorkspaceResponse {
    String slug;
    String name;
    String chatModel;
    String chatProvider;
    String chatMode;
    Double temperature;
    Integer topN;
    Double similarityThreshold;
    Integer openAiHistory;
    String openAiPrompt;
    String queryRefusalResponse;
    Integer documentCount;
    List<AiDocumentResponse> documents;
}