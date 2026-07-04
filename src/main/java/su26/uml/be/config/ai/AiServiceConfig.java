package su26.uml.be.config.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.config.anythingllm.AnythingLlmProperties;
import su26.uml.be.service.ai.UmlArchitect;

@Configuration
@Slf4j
public class AiServiceConfig {

    @Value("classpath:ai/system_prompt.txt")
    private Resource systemPromptResource;

    @Bean
    public ChatLanguageModel chatLanguageModel(AnythingLlmProperties properties) {
        // Quay lại URL chuẩn của AnythingLLM OpenAI API (Khúc đầu làm được)
        String baseUrl = properties.baseUrl().replaceAll("/+$", "");
        String openAiUrl = baseUrl.endsWith("/api") ? baseUrl + "/v1/openai" : baseUrl + "/api/v1/openai";
        
        log.info("Cấu hình AI Model (LangChain4j): URL={}, Model (Workspace)={}", openAiUrl, properties.workspaceSlug());

        return OpenAiChatModel.builder()
                .baseUrl(openAiUrl)
                .apiKey(properties.apiKey())
                .modelName(properties.workspaceSlug())
                .temperature(0.0)
                .maxTokens(4096)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public UmlArchitect umlArchitect(ChatLanguageModel chatLanguageModel) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);

        return AiServices.builder(UmlArchitect.class)
                .chatLanguageModel(chatLanguageModel)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .build();
    }
}
