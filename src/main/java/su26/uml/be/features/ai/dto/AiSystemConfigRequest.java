package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiSystemConfigRequest {
    String llmProvider;
    String baseUrl;
    String apiKey;
    String model;
    String embeddingProvider;
    String embeddingModel;
    String vectorDb;
    String vectorDbEndpoint;
    String vectorDbApiKey;
    Integer documentChunkSize;
    Integer documentChunkOverlap;
}