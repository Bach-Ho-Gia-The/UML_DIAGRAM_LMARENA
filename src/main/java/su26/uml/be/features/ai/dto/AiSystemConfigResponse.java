package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiSystemConfigResponse {
    String llmProvider;
    String model;
    String baseUrl;
    String embeddingProvider;
    String embeddingModel;
    String vectorDb;
    String vectorDbEndpoint;
    String anythingLlmBaseUrl;
    Integer documentChunkSize;
    Integer documentChunkOverlap;
    Boolean hasApiKey;
}