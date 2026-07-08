package su26.uml.be.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import su26.uml.be.dto.response.AiEdgeDto;
import su26.uml.be.dto.response.AiNodeDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiagramChatRequest {

        String sessionId;

        String sheetId;

        @NotBlank(message = "CHAT_MESSAGE_REQUIRED")
    String message;

    List<AiNodeDto> currentNodes;
    List<AiEdgeDto> currentEdges;
}