package su26.uml.be.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import su26.uml.be.entity.AiSourceDocument;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiagramChatResponse {

    AiResponseKind kind;

    String diagramType;

    String summary;

    @JsonAlias({"text", "message", "content", "response"})
    String answer;

    String sessionId;

    @Builder.Default
    List<AiNodeDto> nodes = List.of();

    @Builder.Default
    List<AiEdgeDto> edges = List.of();

    @Builder.Default
    List<AiQuestionDto> questions = List.of();

    @Builder.Default
    List<AiSourceDocument> sources = List.of();
}
