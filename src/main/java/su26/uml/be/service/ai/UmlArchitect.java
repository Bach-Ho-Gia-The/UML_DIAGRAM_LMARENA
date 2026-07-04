package su26.uml.be.service.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import su26.uml.be.dto.response.DiagramChatResponse;

public interface UmlArchitect {

    String chat(@UserMessage String message);
}
