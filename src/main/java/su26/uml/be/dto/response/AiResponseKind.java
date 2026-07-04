package su26.uml.be.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AiResponseKind {
    @JsonProperty("diagram") DIAGRAM,
    @JsonProperty("questions") QUESTIONS,
    @JsonProperty("reply") REPLY
}
