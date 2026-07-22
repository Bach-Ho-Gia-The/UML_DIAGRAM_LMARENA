package su26.uml.be.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anything-llm")
public record AnythingLlmProperties(
        String baseUrl,
        String apiKey,
        String workspaceSlug,
        String modelName,
        String version
) {
    public AnythingLlmProperties {
        if (version == null) version = "1.14.0";
    }
}