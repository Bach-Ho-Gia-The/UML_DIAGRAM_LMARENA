package su26.uml.be.config.anythingllm;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.dto.response.AnythingLlmChatResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnythingLlmClient {

    private final WebClient anythingLlmWebClient;
    private final AnythingLlmProperties properties;

    // ─── Chat (existing) ─────────────────────────────────────────
    public AnythingLlmChatResponse chat(String message, String sessionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("mode", "chat"); 
        body.put("sessionId", sessionId);

        // In ra log để bạn kiểm tra (xem trong console IDE)
        System.out.println("Gửi tới AnythingLLM Workspace: " + properties.workspaceSlug());
        System.out.println("Body: " + body);

        return anythingLlmWebClient.post()
                .uri("/v1/workspace/{slug}/chat", properties.workspaceSlug())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AnythingLlmChatResponse.class)
                .block(Duration.ofMinutes(5));
    }

    // ─── System Config ───────────────────────────────────────────
    public Map<String, Object> getSystemConfig() {
        return anythingLlmWebClient.get()
                .uri("/v1/system")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(10));
    }

    public List<String> getSystemPreferences() {
        return List.of(
            "ollama", "openai", "anthropic", "google", "mistral",
            "groq", "together", "deepseek", "openrouter", "perplexity",
            "azure", "cohere", "fireworks", "novita"
        );
    }

    public Map<String, Object> updateSystemConfig(Map<String, Object> config) {
        return anythingLlmWebClient.post()
                .uri("/v1/system/update-env")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(config)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(15));
    }

    public Map<String, Object> applySystemSettings() {
        return anythingLlmWebClient.post()
                .uri("/v1/system/update-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(15));
    }

    // ─── Model Auto-Detect ───────────────────────────────────────
    public List<String> fetchOllamaModels(String baseUrl) {
        try {
            String url = baseUrl != null && !baseUrl.isBlank()
                    ? baseUrl.replaceAll("/+$", "") + "/api/tags"
                    : "http://localhost:11434/api/tags";

            WebClient ollamaClient = WebClient.builder()
                    .baseUrl(url)
                    .build();

            Map<String, Object> raw = ollamaClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(10));

            List<Map<String, Object>> models = (List<Map<String, Object>>) raw.getOrDefault("models", List.of());
            return models.stream()
                    .map(m -> str(m.get("name")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch Ollama models from {}", baseUrl, e);
            return List.of();
        }
    }

    public List<String> fetchOpenAiCompatibleModels(String baseUrl) {
        try {
            String url = baseUrl != null && !baseUrl.isBlank()
                    ? baseUrl.replaceAll("/+$", "") + "/v1/models"
                    : "http://localhost:1234/v1/models";

            WebClient openAiClient = WebClient.builder()
                    .baseUrl(url)
                    .build();

            Map<String, Object> raw = openAiClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(10));

            List<Map<String, Object>> models = (List<Map<String, Object>>) raw.getOrDefault("data", List.of());
            return models.stream()
                    .map(m -> str(m.get("id")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch OpenAI-compatible models from {}", baseUrl, e);
            return List.of();
        }
    }

    private String str(Object value) {
        return value != null ? value.toString() : null;
    }

    // ─── Connection Test ─────────────────────────────────────────
    public Map<String, Object> testConnection() {
        try {
            return anythingLlmWebClient.get()
                    .uri("/ping")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("online", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    // ─── Workspaces ──────────────────────────────────────────────
    public Map<String, Object> createWorkspace(Map<String, Object> body) {
        return anythingLlmWebClient.post()
                .uri("/v1/workspace/new")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(15));
    }

    public void deleteWorkspace(String slug) {
        anythingLlmWebClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/v1/workspace/{slug}", slug)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));
    }

    public Map<String, Object> getWorkspaces() {
        return anythingLlmWebClient.get()
                .uri("/v1/workspaces")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(10));
    }

    public Map<String, Object> getWorkspaceBySlug(String slug) {
        return anythingLlmWebClient.get()
                .uri("/v1/workspace/{slug}", slug)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(10));
    }

    public Map<String, Object> updateWorkspace(Map<String, Object> settings, String slug) {
        return anythingLlmWebClient.post()
                .uri("/v1/workspace/{slug}/update", slug)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(settings)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(15));
    }

    // ─── Documents ───────────────────────────────────────────────
    public Map<String, Object> uploadDocument(MultipartFile file, String workspaceSlug) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            builder.part("file", resource, MediaType.APPLICATION_OCTET_STREAM);
            builder.part("addToWorkspaces", workspaceSlug);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        return anythingLlmWebClient.post()
                .uri("/v1/document/upload")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(120));
    }

    public void deleteDocument(String documentPath) {
        Map<String, Object> body = new HashMap<>();
        body.put("names", List.of(documentPath));

        anythingLlmWebClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/v1/system/remove-documents")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));
    }

    public Map<String, Object> reEmbedDocuments(String workspaceSlug) {
        return anythingLlmWebClient.post()
                .uri("/v1/workspace/{slug}/update-embeddings", workspaceSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(120));
    }

    public Map<String, Object> getVersion() {
        return anythingLlmWebClient.get()
                .uri("/")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block(Duration.ofSeconds(10));
    }
}
