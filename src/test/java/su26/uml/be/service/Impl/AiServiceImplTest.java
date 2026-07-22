package su26.uml.be.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import su26.uml.be.infrastructure.ai.AnythingLlmClient;
import su26.uml.be.infrastructure.ai.AnythingLlmProperties;
import su26.uml.be.features.ai.dto.AiSystemConfigRequest;
import su26.uml.be.features.ai.dto.AiWorkspaceUpdateRequest;
import su26.uml.be.features.ai.service.Impl.AiServiceImpl;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.common.security.AesEncryption;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    AnythingLlmClient anythingLlmClient;
    @Mock
    AnythingLlmProperties properties;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @Mock
    AesEncryption aesEncryption;

    AiServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiServiceImpl(anythingLlmClient, properties, redisTemplate, aesEncryption);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(aesEncryption.encrypt(anyString())).thenAnswer(i -> i.getArgument(0));
        lenient().when(aesEncryption.decrypt(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void getProviderModels_ollama_readsBasePathFromConfig() {
        when(anythingLlmClient.getSystemConfig()).thenReturn(
                Map.of("settings", Map.of("OLLAMA_BASE_PATH", "http://ollama:11434"))
        );
        when(anythingLlmClient.fetchOllamaModels("http://ollama:11434")).thenReturn(List.of("llama3", "mistral"));

        var result = service.getProviderModels("ollama", null, null);

        assertNotNull(result);
        assertEquals(List.of("llama3", "mistral"), result.getResult());
    }

    @Test
    void getProviderModels_ollama_usesDefaultBasePathWhenConfigMissing() {
        when(anythingLlmClient.getSystemConfig()).thenReturn(Map.of("settings", Map.of()));
        when(anythingLlmClient.fetchOllamaModels("http://localhost:11434")).thenReturn(List.of("llama3"));

        var result = service.getProviderModels("ollama", null, null);

        assertNotNull(result);
        assertEquals(List.of("llama3"), result.getResult());
    }

    @Test
    void getProviderModels_ollama_usesProvidedBasePath() {
        when(anythingLlmClient.fetchOllamaModels("http://custom:11434")).thenReturn(List.of("custom-model"));

        var result = service.getProviderModels("ollama", "http://custom:11434", null);

        assertNotNull(result);
        assertEquals(List.of("custom-model"), result.getResult());
        verify(anythingLlmClient, never()).getSystemConfig();
    }

    @Test
    void getProviderModels_groq_usesRedisKey() {
        when(valueOps.get("ai:apikey:groq")).thenReturn("gsk_test_key");
        when(anythingLlmClient.fetchOpenAiCompatibleModels("https://api.groq.com/openai/v1", "gsk_test_key"))
                .thenReturn(List.of("mixtral", "llama3"));

        var result = service.getProviderModels("groq", null, null);

        assertNotNull(result);
        assertEquals(List.of("mixtral", "llama3"), result.getResult());
    }

    @Test
    void getProviderModels_groq_usesInlineApiKey() {
        when(anythingLlmClient.fetchOpenAiCompatibleModels("https://api.groq.com/openai/v1", "gsk_inline"))
                .thenReturn(List.of("mixtral"));

        var result = service.getProviderModels("groq", null, "gsk_inline");

        assertNotNull(result);
        assertEquals(List.of("mixtral"), result.getResult());
        verify(valueOps).set("ai:apikey:groq", "gsk_inline", java.time.Duration.ofDays(365));
    }

    @Test
    void getProviderModels_groq_redisMiss_fallbackToEnv() {
        when(valueOps.get("ai:apikey:groq")).thenReturn(null);
        when(anythingLlmClient.getSystemConfig()).thenReturn(
                Map.of("settings", Map.of("GroqApiKey", "gsk_actual_key"))
        );
        when(anythingLlmClient.fetchOpenAiCompatibleModels("https://api.groq.com/openai/v1", "gsk_actual_key"))
                .thenReturn(List.of("mixtral"));

        var result = service.getProviderModels("groq", null, null);

        assertNotNull(result);
        assertEquals(List.of("mixtral"), result.getResult());
        verify(valueOps).set("ai:apikey:groq", "gsk_actual_key", java.time.Duration.ofDays(365));
    }

    @Test
    void getProviderModels_groq_redisMiss_envReturnsBooleanSentinel_throws() {
        when(valueOps.get("ai:apikey:groq")).thenReturn(null);
        when(anythingLlmClient.getSystemConfig()).thenReturn(
                Map.of("settings", Map.of("GroqApiKey", "true"))
        );

        var ex = assertThrows(AppException.class, () -> service.getProviderModels("groq", null, null));
        assertEquals(ErrorCode.AI_PROVIDER_API_KEY_MISSING, ex.getErrorCode());
    }

    @Test
    void getProviderModels_groq_redisMiss_envReturnsFalse_throws() {
        when(valueOps.get("ai:apikey:groq")).thenReturn(null);
        when(anythingLlmClient.getSystemConfig()).thenReturn(
                Map.of("settings", Map.of("GroqApiKey", "false"))
        );

        var ex = assertThrows(AppException.class, () -> service.getProviderModels("groq", null, null));
        assertEquals(ErrorCode.AI_PROVIDER_API_KEY_MISSING, ex.getErrorCode());
    }

    @Test
    void getProviderModels_unknownProvider_throws() {
        var ex = assertThrows(AppException.class, () -> service.getProviderModels("unknown", null, null));
        assertEquals(ErrorCode.AI_PROVIDER_MODELS_FAILED, ex.getErrorCode());
    }

    @Test
    void getProviderModels_groq_usesProvidedBasePath() {
        when(valueOps.get("ai:apikey:groq")).thenReturn("gsk_key");
        when(anythingLlmClient.fetchOpenAiCompatibleModels("https://custom-groq.com/v1", "gsk_key"))
                .thenReturn(List.of("mixtral"));

        var result = service.getProviderModels("groq", "https://custom-groq.com/v1", null);

        assertNotNull(result);
        assertEquals(List.of("mixtral"), result.getResult());
    }

    @Test
    void getProviderModels_ollama_fetchException_throws() {
        when(anythingLlmClient.getSystemConfig()).thenReturn(Map.of("settings", Map.of()));
        when(anythingLlmClient.fetchOllamaModels(anyString())).thenThrow(new RuntimeException("Connection refused"));

        var ex = assertThrows(AppException.class, () -> service.getProviderModels("ollama", null, null));
        assertEquals(ErrorCode.AI_PROVIDER_MODELS_FAILED, ex.getErrorCode());
    }

    @Test
    void getProviderModels_groq_redisAndEnvBothMissing_throws() {
        when(valueOps.get("ai:apikey:groq")).thenReturn(null);
        when(anythingLlmClient.getSystemConfig()).thenReturn(
                Map.of("settings", Map.of("GroqApiKey", false))
        );

        var ex = assertThrows(AppException.class, () -> service.getProviderModels("groq", null, null));
        assertEquals(ErrorCode.AI_PROVIDER_API_KEY_MISSING, ex.getErrorCode());
    }

    @Test
    void getProviderModels_groq_redisError_fallsBackToEnv() {
        when(valueOps.get("ai:apikey:groq")).thenThrow(new RuntimeException("Redis down"));
        when(anythingLlmClient.getSystemConfig()).thenReturn(
                Map.of("settings", Map.of("GroqApiKey", "gsk_fallback"))
        );
        when(anythingLlmClient.fetchOpenAiCompatibleModels("https://api.groq.com/openai/v1", "gsk_fallback"))
                .thenReturn(List.of("mixtral"));

        var result = service.getProviderModels("groq", null, null);

        assertNotNull(result);
        assertEquals(List.of("mixtral"), result.getResult());
    }

    // ─── Workspace update verification ──────────────────────────────

    @Test
    void getProviderModels_ollama_passesDockerInternalUrl() {
        when(anythingLlmClient.fetchOllamaModels("http://host.docker.internal:11434"))
                .thenReturn(List.of("llama3"));

        var result = service.getProviderModels("ollama", "http://host.docker.internal:11434", null);

        assertNotNull(result);
        assertEquals(List.of("llama3"), result.getResult());
        verify(anythingLlmClient, never()).getSystemConfig();
    }

    @Test
    void updateWorkspace_updatesAndVerifiesWorkspace() {
        String slug = "test-ws";
        var request = AiWorkspaceUpdateRequest.builder()
                .chatProvider("groq")
                .model("mixtral")
                .build();

        when(properties.workspaceSlug()).thenReturn(slug);
        when(anythingLlmClient.updateWorkspace(anyMap(), eq(slug)))
                .thenReturn(Map.of());
        when(anythingLlmClient.getWorkspaceBySlug(slug))
                .thenReturn(Map.of("workspace", Map.of(
                        "slug", slug,
                        "name", "Test",
                        "chatModel", "mixtral",
                        "chatProvider", "groq"
                )));

        var result = service.updateWorkspace(request, null);

        assertNotNull(result);
        assertNotNull(result.getResult());
        assertEquals("groq", result.getResult().getChatProvider());
        assertEquals("mixtral", result.getResult().getChatModel());
        verify(anythingLlmClient).updateWorkspace(anyMap(), eq(slug));
        verify(anythingLlmClient).getWorkspaceBySlug(slug);
    }

    @Test
    void updateWorkspace_logsMismatchWhenProviderDiffers() {
        String slug = "test-ws";
        var request = AiWorkspaceUpdateRequest.builder()
                .chatProvider("groq")
                .model("mixtral")
                .build();

        when(properties.workspaceSlug()).thenReturn(slug);
        when(anythingLlmClient.updateWorkspace(anyMap(), eq(slug)))
                .thenReturn(Map.of());
        when(anythingLlmClient.getWorkspaceBySlug(slug))
                .thenReturn(Map.of("workspace", Map.of(
                        "slug", slug,
                        "name", "Test",
                        "chatModel", "mixtral",
                        "chatProvider", "ollama"
                )));

        var result = service.updateWorkspace(request, null);

        assertNotNull(result);
        assertEquals("ollama", result.getResult().getChatProvider());
        verify(anythingLlmClient).getWorkspaceBySlug(slug);
    }

    @Test
    void updateWorkspace_throwsOnFailure() {
        when(properties.workspaceSlug()).thenReturn("test-slug");
        when(anythingLlmClient.updateWorkspace(anyMap(), anyString()))
                .thenThrow(new RuntimeException("AnythingLLM error"));

        var request = AiWorkspaceUpdateRequest.builder().chatProvider("groq").build();

        assertThrows(AppException.class, () -> service.updateWorkspace(request, null));
    }

    // ─── System config build ────────────────────────────────────────

    @Test
    void updateSystemConfig_ollama_translatesLocalhostToDockerInternal() {
        var request = AiSystemConfigRequest.builder()
                .llmProvider("ollama")
                .baseUrl("http://localhost:11434")
                .model("llama3")
                .build();

        when(anythingLlmClient.getSystemConfig()).thenReturn(Map.of("settings", Map.of()));

        service.updateSystemConfig(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(anythingLlmClient).updateSystemConfig(captor.capture());
        Map<String, Object> env = captor.getValue();
        assertEquals("http://host.docker.internal:11434", env.get("OllamaLLMBasePath"));
    }

    @Test
    void updateSystemConfig_ollama_keepsNonLocalhostUrl() {
        var request = AiSystemConfigRequest.builder()
                .llmProvider("ollama")
                .baseUrl("http://ollama-server:11434")
                .model("llama3")
                .build();

        when(anythingLlmClient.getSystemConfig()).thenReturn(Map.of("settings", Map.of()));

        service.updateSystemConfig(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(anythingLlmClient).updateSystemConfig(captor.capture());
        Map<String, Object> env = captor.getValue();
        assertEquals("http://ollama-server:11434", env.get("OllamaLLMBasePath"));
    }

    @Test
    void updateSystemConfig_groq_preservesLocalhostUrl() {
        var request = AiSystemConfigRequest.builder()
                .llmProvider("groq")
                .baseUrl("http://localhost:11434")
                .model("mixtral")
                .build();

        when(anythingLlmClient.getSystemConfig()).thenReturn(Map.of("settings", Map.of()));

        service.updateSystemConfig(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(anythingLlmClient).updateSystemConfig(captor.capture());
        Map<String, Object> env = captor.getValue();
        assertEquals("http://localhost:11434", env.get("OpenAiBasePath"));
    }
}
