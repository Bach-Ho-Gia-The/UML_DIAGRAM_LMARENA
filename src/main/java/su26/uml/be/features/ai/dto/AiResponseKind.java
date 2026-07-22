package su26.uml.be.features.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AiResponseKind {
    @JsonProperty("diagram") DIAGRAM,
    @JsonProperty("questions") QUESTIONS,
    @JsonProperty("reply") REPLY
}