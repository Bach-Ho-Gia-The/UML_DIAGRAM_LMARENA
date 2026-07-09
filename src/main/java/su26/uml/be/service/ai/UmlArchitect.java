package su26.uml.be.service.ai;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

public interface UmlArchitect {

    Result<String> chat(@UserMessage String message);
}
