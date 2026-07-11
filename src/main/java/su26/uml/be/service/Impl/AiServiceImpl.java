package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.annotation.Auditable;
import su26.uml.be.config.anythingllm.AnythingLlmClient;
import su26.uml.be.config.anythingllm.AnythingLlmProperties;
import su26.uml.be.dto.request.AiCreateWorkspaceRequest;
import su26.uml.be.dto.request.AiDocumentDeleteRequest;
import su26.uml.be.dto.request.AiSystemConfigRequest;
import su26.uml.be.dto.request.AiWorkspaceUpdateRequest;
import su26.uml.be.dto.response.*;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.security.AesEncryption;
import su26.uml.be.service.AiService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AiServiceImpl implements AiService {

    AnythingLlmClient anythingLlmClient;
    AnythingLlmProperties properties;
    StringRedisTemplate redisTemplate;
    AesEncryption aesEncryption;
    static final Path DOC_CONTENT_DIR = Paths.get("data", "doc-content");
    static final String AI_APIKEY_PREFIX = "ai:apikey:";
    static final Duration AI_APIKEY_TTL = Duration.ofDays(365);

    @Override
    public ApiResponse<AiSystemConfigResponse> getSystemConfig() {
        try {
            Map<String, Object> raw = anythingLlmClient.getSystemConfig();
            return ApiResponse.success("OK", mapToSystemConfig(raw));
        } catch (Exception e) {
            log.error("Failed to fetch system config from AnythingLLM", e);
            throw new AppException(ErrorCode.AI_SYSTEM_CONFIG_FAILED);
        }
    }

    @Override
    @Auditable(action = "AI_WORKSPACE_CREATE", targetType = "AI_WORKSPACE", targetId = "#request.name")
    public ApiResponse<Void> createWorkspace(AiCreateWorkspaceRequest request) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", request.getName());
            anythingLlmClient.createWorkspace(body);
            return ApiResponse.success("Tạo workspace thành công");
        } catch (Exception e) {
            log.error("Failed to create workspace", e);
            throw new AppException(ErrorCode.AI_WORKSPACE_CREATE_FAILED);
        }
    }

    @Override
    @Auditable(action = "AI_WORKSPACE_DELETE", targetType = "AI_WORKSPACE", targetId = "#slug")
    public ApiResponse<Void> deleteWorkspace(String slug) {
        try {
            anythingLlmClient.deleteWorkspace(slug);
            return ApiResponse.success("Xoá workspace thành công");
        } catch (Exception e) {
            log.error("Failed to delete workspace", e);
            throw new AppException(ErrorCode.AI_WORKSPACE_DELETE_FAILED);
        }
    }

    @Override
    @Auditable(action = "AI_CONFIG_UPDATE", targetType = "AI_CONFIG")
    public ApiResponse<AiSystemConfigResponse> updateSystemConfig(AiSystemConfigRequest request) {
        try {
            Map<String, Object> envConfig = buildSystemEnvConfig(request);
            anythingLlmClient.updateSystemConfig(envConfig);

            if (request.getApiKey() != null && request.getLlmProvider() != null) {
                String provider = request.getLlmProvider().toLowerCase();
                String encrypted = aesEncryption.encrypt(request.getApiKey());
                redisTemplate.opsForValue().set(
                    AI_APIKEY_PREFIX + provider,
                    encrypted,
                    AI_APIKEY_TTL
                );
                log.debug("Stored encrypted API key for provider {} in Redis", provider);
            }

            return getSystemConfig();
        } catch (Exception e) {
            log.error("Failed to update system config", e);
            throw new AppException(ErrorCode.AI_UPDATE_CONFIG_FAILED);
        }
    }

    @Override
    public ApiResponse<List<String>> getSupportedProviders() {
        List<String> providers = anythingLlmClient.getSystemPreferences();
        return ApiResponse.success("OK", providers);
    }

    @Override
    public ApiResponse<AiTestConnectionResponse> testConnection() {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> result = anythingLlmClient.testConnection();
            long latency = System.currentTimeMillis() - start;
            boolean connected = result != null
                    && result.containsKey("online")
                    && Boolean.TRUE.equals(result.get("online"));
            return ApiResponse.success("OK", AiTestConnectionResponse.builder()
                    .connected(connected)
                    .latencyMs(latency)
                    .build());
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return ApiResponse.success("OK", AiTestConnectionResponse.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .build());
        }
    }

    @Override
    public ApiResponse<AiWorkspaceResponse> getWorkspaceBySlug(String slug) {
        try {
            String resolved = slug != null && !slug.isBlank() ? slug : properties.workspaceSlug();
            Map<String, Object> raw = anythingLlmClient.getWorkspaceBySlug(resolved);
            Map<String, Object> workspace = extractWorkspace(raw);
            return ApiResponse.success("OK", mapToWorkspaceResponse(workspace));
        } catch (Exception e) {
            log.error("Failed to fetch workspace from AnythingLLM", e);
            throw new AppException(ErrorCode.AI_WORKSPACE_NOT_FOUND);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @Auditable(action = "AI_WORKSPACE_UPDATE", targetType = "AI_WORKSPACE", targetId = "#slug")
    public ApiResponse<AiWorkspaceResponse> updateWorkspace(AiWorkspaceUpdateRequest request, String slug) {
        try {
            String resolved = slug != null && !slug.isBlank() ? slug : properties.workspaceSlug();
            Map<String, Object> settings = new HashMap<>();
            if (request.getModel() != null) settings.put("chatModel", request.getModel());
            if (request.getChatProvider() != null) settings.put("chatProvider", request.getChatProvider());
            if (request.getChatMode() != null) settings.put("chatMode", request.getChatMode());
            if (request.getTemperature() != null) settings.put("openAiTemp", request.getTemperature());
            if (request.getTopN() != null) settings.put("topN", request.getTopN());
            if (request.getSimilarityThreshold() != null) settings.put("similarityThreshold", request.getSimilarityThreshold());
            if (request.getOpenAiHistory() != null) settings.put("openAiHistory", request.getOpenAiHistory());
            if (request.getOpenAiPrompt() != null) settings.put("openAiPrompt", request.getOpenAiPrompt());
            if (request.getQueryRefusalResponse() != null) settings.put("queryRefusalResponse", request.getQueryRefusalResponse());

            anythingLlmClient.updateWorkspace(settings, resolved);

            Map<String, Object> verify = anythingLlmClient.getWorkspaceBySlug(resolved);
            Map<String, Object> workspace = extractWorkspace(verify);
            String actualProvider = str(workspace.get("chatProvider"));
            String actualModel = str(workspace.get("chatModel"));

            if (request.getChatProvider() != null && !request.getChatProvider().equalsIgnoreCase(actualProvider)) {
                log.warn("Workspace {} chatProvider mismatch: requested={}, actual={}", resolved, request.getChatProvider(), actualProvider);
            }
            if (request.getModel() != null && !request.getModel().equals(actualModel)) {
                log.warn("Workspace {} chatModel mismatch: requested={}, actual={}", resolved, request.getModel(), actualModel);
            }
            return ApiResponse.success("Cập nhật workspace thành công", mapToWorkspaceResponse(workspace));
        } catch (Exception e) {
            log.error("Failed to update workspace", e);
            throw new AppException(ErrorCode.AI_WORKSPACE_UPDATE_FAILED);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ApiResponse<List<AiWorkspaceListItem>> getWorkspaces() {
        try {
            Map<String, Object> raw = anythingLlmClient.getWorkspaces();
            List<Map<String, Object>> workspaces = (List<Map<String, Object>>) raw.getOrDefault("workspaces", List.of());
            List<AiWorkspaceListItem> result = workspaces.stream()
                    .map(ws -> AiWorkspaceListItem.builder()
                            .slug(str(ws.get("slug")))
                            .name(str(ws.get("name")))
                            .build())
                    .collect(Collectors.toList());
            return ApiResponse.success("OK", result);
        } catch (Exception e) {
            log.error("Failed to fetch workspaces from AnythingLLM", e);
            throw new AppException(ErrorCode.AI_WORKSPACE_NOT_FOUND);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ApiResponse<List<AiDocumentResponse>> getDocuments(String workspaceSlug) {
        try {
            String slug = workspaceSlug != null && !workspaceSlug.isBlank()
                    ? workspaceSlug : properties.workspaceSlug();
            Map<String, Object> raw = anythingLlmClient.getWorkspaceBySlug(slug);
            Map<String, Object> workspace = extractWorkspace(raw);
            List<Map<String, Object>> docs = (List<Map<String, Object>>) workspace.getOrDefault("documents", List.of());
            List<AiDocumentResponse> result = docs.stream()
                    .map(this::mapToDocumentResponse)
                    .collect(Collectors.toList());
            return ApiResponse.success("OK", result);
        } catch (Exception e) {
            log.error("Failed to fetch documents", e);
            throw new AppException(ErrorCode.AI_SYSTEM_CONFIG_FAILED);
        }
    }

    @Override
    @Auditable(action = "AI_DOCUMENT_UPLOAD", targetType = "AI_DOCUMENT", targetId = "#file.originalFilename")
    public ApiResponse<Void> uploadDocument(MultipartFile file, String workspaceSlug) {
        try {
            String slug = workspaceSlug != null && !workspaceSlug.isBlank()
                    ? workspaceSlug : properties.workspaceSlug();
            anythingLlmClient.uploadDocument(file, slug);
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Path wsDir = DOC_CONTENT_DIR.resolve(slug);
            Files.createDirectories(wsDir);
            Files.writeString(wsDir.resolve(file.getOriginalFilename()), content);
            return ApiResponse.success("Tải document lên thành công");
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            throw new AppException(ErrorCode.AI_DOCUMENT_UPLOAD_FAILED);
        }
    }

    @Override
    public ApiResponse<String> getDocumentContent(String workspace, String filename) {
        try {
            Path wsDir = DOC_CONTENT_DIR.resolve(workspace);
            Path filePath = wsDir.resolve(filename);
            log.info("getDocumentContent: workspace={}, filename={}, resolvedPath={}", workspace, filename, filePath.toAbsolutePath());
            if (!Files.exists(filePath)) {
                log.warn("Document content not found at {}", filePath.toAbsolutePath());
                throw new AppException(ErrorCode.AI_DOCUMENT_CONTENT_NOT_FOUND);
            }
            String content = Files.readString(filePath);
            return ApiResponse.success("OK", content);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to read document content for workspace={}, filename={}", workspace, filename, e);
            throw new AppException(ErrorCode.AI_DOCUMENT_CONTENT_NOT_FOUND);
        }
    }

    @Override
    @Auditable(action = "AI_DOCUMENT_DELETE", targetType = "AI_DOCUMENT", targetId = "#request.documentPath")
    public ApiResponse<Void> deleteDocument(AiDocumentDeleteRequest request) {
        try {
            anythingLlmClient.deleteDocument(request.getDocumentPath());
            return ApiResponse.success("Xoá document thành công");
        } catch (Exception e) {
            log.error("Failed to delete document", e);
            throw new AppException(ErrorCode.AI_DOCUMENT_DELETE_FAILED);
        }
    }

    @Override
    @Auditable(action = "AI_DOCUMENT_REEMBED", targetType = "AI_DOCUMENT", targetId = "#workspaceSlug")
    public ApiResponse<Void> reEmbedDocuments(String workspaceSlug) {
        try {
            String slug = workspaceSlug != null && !workspaceSlug.isBlank()
                    ? workspaceSlug : properties.workspaceSlug();
            anythingLlmClient.reEmbedDocuments(slug);
            return ApiResponse.success("Re-embed documents thành công");
        } catch (Exception e) {
            log.error("Failed to re-embed documents", e);
            throw new AppException(ErrorCode.AI_RE_EMBED_FAILED);
        }
    }

    @Override
    public ApiResponse<AiVersionResponse> getVersion() {
        String version = properties.version();
        String llmProvider = null;
        String llmModel = null;

        try {
            Map<String, Object> raw = anythingLlmClient.getVersion();
            if (raw != null && raw.containsKey("version")) {
                version = str(raw.get("version"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch version from AnythingLLM, using config default: {}", version);
        }

        try {
            Map<String, Object> config = anythingLlmClient.getSystemConfig();
            Map<String, Object> settings = (Map<String, Object>) config.get("settings");
            if (settings != null) {
                llmProvider = str(settings.get("LLMProvider"));
                llmModel = str(settings.get("LLMModel"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch system config for version info");
        }

        String baseUrl = properties.baseUrl();
        String environment = (baseUrl != null && baseUrl.contains("onrender.com"))
                ? "Production" : "Development";

        return ApiResponse.success("OK", AiVersionResponse.builder()
                .version(version != null ? version : "unknown")
                .llmProvider(llmProvider)
                .model(llmModel)
                .environment(environment)
                .build());
    }

    @Override
    public ApiResponse<List<String>> getProviderModels(String provider, String basePath, String apiKey) {
        try {
            String p = provider != null ? provider.toLowerCase() : "";

            if ("ollama".equals(p) && (basePath == null || basePath.isBlank())) {
                try {
                    Map<String, Object> config = anythingLlmClient.getSystemConfig();
                    Map<String, Object> settings = (Map<String, Object>) config.get("settings");
                    if (settings != null) {
                        String path = str(settings.get("OLLAMA_BASE_PATH"));
                        if (path != null && !path.isBlank()) basePath = path;
                    }
                } catch (Exception e) {
                    log.warn("Could not read OLLAMA_BASE_PATH from system config", e);
                }
                if (basePath == null || basePath.isBlank()) {
                    basePath = "http://localhost:11434";
                }
            }

            // apiKey from request body: save to Redis immediately, then use it
            boolean hasInlineKey = apiKey != null && !apiKey.isBlank() && !"ollama".equals(p);
            if (hasInlineKey) {
                String redisKey = AI_APIKEY_PREFIX + p;
                String encrypted = aesEncryption.encrypt(apiKey);
                redisTemplate.opsForValue().set(redisKey, encrypted, AI_APIKEY_TTL);
                log.debug("Saved API key from request for provider {} to Redis", p);
            }

            List<String> models;
            if ("ollama".equals(p)) {
                models = anythingLlmClient.fetchOllamaModels(basePath);
            } else if ("groq".equals(p)) {
                if (!hasInlineKey) apiKey = resolveApiKey("groq", "GroqApiKey");
                if (apiKey == null) throw new AppException(ErrorCode.AI_PROVIDER_API_KEY_MISSING);
                if (basePath == null || basePath.isBlank()) {
                    basePath = "https://api.groq.com/openai/v1";
                }
                models = anythingLlmClient.fetchOpenAiCompatibleModels(basePath, apiKey);
            } else {
                throw new AppException(ErrorCode.AI_PROVIDER_MODELS_FAILED);
            }

            return ApiResponse.success("OK", models);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch models for provider {}", provider, e);
            throw new AppException(ErrorCode.AI_PROVIDER_MODELS_FAILED);
        }
    }

    private String resolveApiKey(String provider, String envKey) {
        String redisKey = AI_APIKEY_PREFIX + provider;
        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null && !cached.isBlank()) {
                String decrypted = aesEncryption.decrypt(cached);
                log.debug("Found and decrypted API key for provider {} in Redis", provider);
                return decrypted;
            }
        } catch (Exception e) {
            log.warn("Could not read/decrypt API key from Redis for provider {}", provider, e);
        }

        String fromEnv = readEnvFromSystemConfig(envKey);
        if (fromEnv != null) {
            if ("true".equalsIgnoreCase(fromEnv) || "false".equalsIgnoreCase(fromEnv)) {
                log.warn("AnythingLLM returned sentinel value '{}' for {} (key not exposed by API), cannot use for auto-detect", fromEnv, envKey);
                return null;
            }
            String encrypted = aesEncryption.encrypt(fromEnv);
            redisTemplate.opsForValue().set(redisKey, encrypted, AI_APIKEY_TTL);
            return fromEnv;
        }
        return null;
    }

    private String readEnvFromSystemConfig(String envKey) {
        try {
            Map<String, Object> config = anythingLlmClient.getSystemConfig();
            Map<String, Object> settings = (Map<String, Object>) config.get("settings");
            if (settings != null) {
                String val = str(settings.get(envKey));
                if (val != null && !val.isBlank() && !"false".equalsIgnoreCase(val)) return val;
            }
        } catch (Exception e) {
            log.warn("Could not read {} from system config", envKey, e);
        }
        return null;
    }

    // ─── Private helpers ─────────────────────────────────────────

    // ─── Provider env var mapping ──────────────────────────────
    private static final Map<String, String> LLM_API_KEY_MAP = Map.ofEntries(
            Map.entry("openai", "OpenAiKey"),
            Map.entry("anthropic", "AnthropicApiKey"),
            Map.entry("azure", "AzureOpenAiKey"),
            Map.entry("mistral", "MistralApiKey"),
            Map.entry("groq", "GroqApiKey"),
            Map.entry("together", "TogetherAiApiKey"),
            Map.entry("deepseek", "DeepSeekApiKey"),
            Map.entry("openrouter", "OpenRouterApiKey"),
            Map.entry("perplexity", "PerplexityApiKey")
    );

    private static final Map<String, String> LLM_BASE_URL_MAP = Map.of(
            "ollama", "OllamaLLMBasePath",
            "openai", "OpenAiBasePath",
            "azure", "AzureOpenAiBasePath",
            "anthropic", "AnthropicBasePath"
    );

    private static final Map<String, String> LLM_MODEL_MAP = Map.of(
            "ollama", "OllamaLLMModelPref",
            "openai", "OpenAiModelPref",
            "anthropic", "AnthropicModelPref",
            "azure", "AzureOpenAiModelPref"
    );

    private static final Map<String, String> EMB_MODEL_MAP = Map.of(
            "openai", "OpenAiEmbeddingModelPref",
            "ollama", "OllamaEmbeddingModelPref",
            "azure", "AzureOpenAiEmbeddingModelPref"
    );

    private static final Map<String, String> VDB_ENDPOINT_MAP = Map.of(
            "qdrant", "QdrantEndpoint",
            "pinecone", "PineConeEndpoint",
            "chroma", "ChromaEndpoint",
            "weaviate", "WeaviateEndpoint",
            "milvus", "MilvusAddress"
    );

    private static final Map<String, String> VDB_API_KEY_MAP = Map.of(
            "qdrant", "QdrantApiKey",
            "chroma", "ChromaApiKey",
            "weaviate", "WeaviateApiKey",
            "pinecone", "PineConeKey"
    );

    private Map<String, Object> buildSystemEnvConfig(AiSystemConfigRequest request) {
        Map<String, Object> env = new HashMap<>();
        String provider = request.getLlmProvider() != null ? request.getLlmProvider().toLowerCase() : "";

        if (request.getLlmProvider() != null) {
            env.put("LLMProvider", request.getLlmProvider());
        }

        if (request.getBaseUrl() != null) {
            String url = request.getBaseUrl();
            if ("ollama".equals(provider)) {
                url = url.replace("localhost", "host.docker.internal");
            }
            env.put(LLM_BASE_URL_MAP.getOrDefault(provider, "OpenAiBasePath"), url);
        }

        String apiKeyEnvKey = LLM_API_KEY_MAP.get(provider);
        if (request.getApiKey() != null) {
            if (apiKeyEnvKey != null) env.put(apiKeyEnvKey, request.getApiKey());
        } else if (apiKeyEnvKey != null) {
            String redisKey = AI_APIKEY_PREFIX + provider;
            try {
                String cached = redisTemplate.opsForValue().get(redisKey);
                if (cached != null && !cached.isBlank()) {
                    String decrypted = aesEncryption.decrypt(cached);
                    env.put(apiKeyEnvKey, decrypted);
                    log.debug("Restored existing API key for {} from Redis", provider);
                }
            } catch (Exception e) {
                log.warn("Could not restore API key from Redis for provider {}", provider, e);
            }
        }

        if (request.getModel() != null) {
            env.put(LLM_MODEL_MAP.getOrDefault(provider, "OpenAiModelPref"), request.getModel());
            env.put("LLMModel", request.getModel());
        }

        if (request.getEmbeddingProvider() != null) {
            env.put("EmbeddingEngine", request.getEmbeddingProvider());
        }

        if (request.getEmbeddingModel() != null) {
            String embProvider = request.getEmbeddingProvider() != null ? request.getEmbeddingProvider().toLowerCase() : "";
            env.put(EMB_MODEL_MAP.getOrDefault(embProvider, "EmbeddingModelPref"), request.getEmbeddingModel());
        }

        if (request.getVectorDb() != null) {
            env.put("VectorDB", request.getVectorDb());
        }

        if (request.getVectorDbEndpoint() != null) {
            String vdb = request.getVectorDb() != null ? request.getVectorDb().toLowerCase() : "";
            env.put(VDB_ENDPOINT_MAP.getOrDefault(vdb, "QdrantEndpoint"), request.getVectorDbEndpoint());
        }

        if (request.getVectorDbApiKey() != null) {
            String vdb = request.getVectorDb() != null ? request.getVectorDb().toLowerCase() : "";
            String key = VDB_API_KEY_MAP.get(vdb);
            if (key != null) env.put(key, request.getVectorDbApiKey());
        }

        if (request.getDocumentChunkSize() != null) {
            env.put("DocumentChunkSize", request.getDocumentChunkSize());
        }
        if (request.getDocumentChunkOverlap() != null) {
            env.put("DocumentChunkOverlap", request.getDocumentChunkOverlap());
        }

        return env;
    }

    @SuppressWarnings("unchecked")
    private AiSystemConfigResponse mapToSystemConfig(Map<String, Object> raw) {
        if (raw == null) return new AiSystemConfigResponse();
        Map<String, Object> settings = (Map<String, Object>) raw.get("settings");
        if (settings == null) return new AiSystemConfigResponse();

        String vectorDb = str(settings.get("VectorDB"));
        String vectorDbEndpoint = resolveVectorDbEndpoint(vectorDb, settings);

        // Read embedding model from provider-specific key, fallback to generic key
        String embProvider = str(settings.get("EmbeddingEngine"));
        String embModelPrefKey = EMB_MODEL_MAP.getOrDefault(
                embProvider != null ? embProvider.toLowerCase() : "",
                "EmbeddingModelPref"
        );
        String embeddingModel = str(settings.get(embModelPrefKey));
        if (embeddingModel == null) embeddingModel = str(settings.get("EmbeddingModelPref"));

        String llmProvider = str(settings.get("LLMProvider"));
        String model = str(settings.get("LLMModel"));
        if (model == null && llmProvider != null) {
            String modelKey = LLM_MODEL_MAP.get(llmProvider.toLowerCase());
            if (modelKey != null) model = str(settings.get(modelKey));
        }

        String baseUrl = null;
        if (llmProvider != null) {
            String baseUrlKey = LLM_BASE_URL_MAP.getOrDefault(llmProvider.toLowerCase(), "OpenAiBasePath");
            baseUrl = str(settings.get(baseUrlKey));
        }

        Integer chunkSize = parseInt(settings.get("DocumentChunkSize"));
        Integer chunkOverlap = parseInt(settings.get("DocumentChunkOverlap"));

        return AiSystemConfigResponse.builder()
                .llmProvider(llmProvider)
                .model(model)
                .baseUrl(baseUrl)
                .embeddingProvider(embProvider)
                .embeddingModel(embeddingModel)
                .vectorDb(vectorDb)
                .vectorDbEndpoint(vectorDbEndpoint)
                .anythingLlmBaseUrl(properties.baseUrl())
                .documentChunkSize(chunkSize)
                .documentChunkOverlap(chunkOverlap)
                .hasApiKey(hasApiKeyConfigured(settings))
                .build();
    }

    private String resolveVectorDbEndpoint(String vectorDb, Map<String, Object> settings) {
        if (vectorDb == null) return null;
        return switch (vectorDb.toLowerCase()) {
            case "qdrant" -> str(settings.get("QdrantEndpoint"));
            case "pinecone" -> str(settings.get("PineConeEndpoint"));
            case "chroma" -> str(settings.get("ChromaEndpoint"));
            case "weaviate" -> str(settings.get("WeaviateEndpoint"));
            case "milvus" -> str(settings.get("MilvusEndpoint"));
            case "astra" -> str(settings.get("AstraDBEndpoint"));
            default -> null;
        };
    }

    private boolean hasApiKeyConfigured(Map<String, Object> settings) {
        String provider = str(settings.get("LLMProvider"));
        if (provider == null) return false;
        String envKey = LLM_API_KEY_MAP.get(provider.toLowerCase());
        if (envKey != null) return isTruthy(settings.get(envKey));
        return true; // providers without API key (e.g., Ollama) are always "configured"
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return !s.isEmpty() && !"false".equalsIgnoreCase(s);
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractWorkspace(Map<String, Object> raw) {
        if (raw == null) return Map.of();
        if (raw.containsKey("workspace")) {
            Object ws = raw.get("workspace");
            if (ws instanceof List && !((List<?>) ws).isEmpty()) {
                return (Map<String, Object>) ((List<?>) ws).get(0);
            } else if (ws instanceof Map) {
                return (Map<String, Object>) ws;
            }
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private AiWorkspaceResponse mapToWorkspaceResponse(Map<String, Object> ws) {
        if (ws == null || ws.isEmpty()) return new AiWorkspaceResponse();

        Object tempObj = ws.get("openAiTemp");
        Double temperature = tempObj instanceof Number ? ((Number) tempObj).doubleValue() : null;

        Object topNObj = ws.get("topN");
        Integer topN = topNObj instanceof Number ? ((Number) topNObj).intValue() : null;

        Object simObj = ws.get("similarityThreshold");
        Double similarityThreshold = simObj instanceof Number ? ((Number) simObj).doubleValue() : null;

        Object historyObj = ws.get("openAiHistory");
        Integer openAiHistory = historyObj instanceof Number ? ((Number) historyObj).intValue() : null;

        List<Map<String, Object>> rawDocs = (List<Map<String, Object>>) ws.getOrDefault("documents", List.of());
        List<AiDocumentResponse> documents = rawDocs.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());

        return AiWorkspaceResponse.builder()
                .slug(str(ws.get("slug")))
                .name(str(ws.get("name")))
                .chatModel(str(ws.get("chatModel")))
                .chatProvider(str(ws.get("chatProvider")))
                .chatMode(str(ws.get("chatMode")))
                .temperature(temperature)
                .topN(topN)
                .similarityThreshold(similarityThreshold)
                .openAiHistory(openAiHistory)
                .openAiPrompt(str(ws.get("openAiPrompt")))
                .queryRefusalResponse(str(ws.get("queryRefusalResponse")))
                .documentCount(documents.size())
                .documents(documents)
                .build();
    }

    @SuppressWarnings("unchecked")
    private AiDocumentResponse mapToDocumentResponse(Map<String, Object> doc) {
        if (doc == null) return new AiDocumentResponse();

        String docId = str(doc.get("docId"));
        if (docId == null || docId.isBlank()) docId = Optional.ofNullable(doc.get("id")).map(Object::toString).orElse(null);

        String rawFilename = str(doc.get("filename"));
        if (rawFilename != null) {
            rawFilename = rawFilename.replaceAll("-[a-f0-9-]{36}\\.json$", "");
        }

        return AiDocumentResponse.builder()
                .docId(docId)
                .filename(rawFilename)
                .docpath(str(doc.get("docpath")))
                .size(doc.get("size") instanceof Number ? ((Number) doc.get("size")).longValue() : null)
                .status(str(doc.get("status")))
                .uploadedAt(str(doc.get("createdAt")))
                .build();
    }

    private String str(Object value) {
        return value != null ? value.toString() : null;
    }

    private Integer parseInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return null;
    }
}
