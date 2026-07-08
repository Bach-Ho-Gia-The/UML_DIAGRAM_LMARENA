package su26.uml.be.service.ai;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.UserMessage;

public interface UmlArchitect {

    ChatResponse chat(@UserMessage String message);
}
