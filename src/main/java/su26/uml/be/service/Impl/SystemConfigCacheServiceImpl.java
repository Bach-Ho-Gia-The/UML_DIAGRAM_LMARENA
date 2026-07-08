package su26.uml.be.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import su26.uml.be.config.anythingllm.AnythingLlmClient;
import su26.uml.be.config.anythingllm.AnythingLlmProperties;
import su26.uml.be.service.SystemConfigCacheService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SystemConfigCacheServiceImpl implements SystemConfigCacheService {

    static final String REDIS_KEY = "system_config:active_model";
    static final long CACHE_TTL_SECONDS = 3600;

    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;
    AnythingLlmClient anythingLlmClient;
    AnythingLlmProperties anythingLlmProperties;

    @Override
    public SystemConfig getActiveModel() {
        String json = redisTemplate.opsForValue().get(REDIS_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(json, SystemConfig.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse SystemConfig cache, will re-sync", e);
            }
        }
        // Cache miss → sync from AnythingLLM
        sync();
        json = redisTemplate.opsForValue().get(REDIS_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(json, SystemConfig.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse SystemConfig even after sync", e);
            }
        }
        // Ultimate fallback: return from properties
        return new SystemConfig(anythingLlmProperties.modelName(), "unknown", anythingLlmProperties.workspaceSlug());
    }

    @Override
    public void sync() {
        try {
            Map<String, Object> config = anythingLlmClient.getSystemConfig();
            if (config == null) {
                log.warn("SystemConfig sync: AnythingLLM returned null config");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) config.get("settings");
            String modelName = null;
            String provider = null;
            if (settings != null) {
                Object llmModel = settings.get("LLMModel");
                if (llmModel instanceof String s) modelName = s;
                Object llmProvider = settings.get("LLMProvider");
                if (llmProvider instanceof String s) provider = s;
            }
            if (modelName == null) modelName = anythingLlmProperties.modelName();
            if (provider == null) provider = "unknown";

            SystemConfig cached = new SystemConfig(modelName, provider, anythingLlmProperties.workspaceSlug());
            String json = objectMapper.writeValueAsString(cached);
            redisTemplate.opsForValue().set(REDIS_KEY, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("SystemConfig cache synced: model={}, provider={}", modelName, provider);
        } catch (Exception e) {
            log.error("Failed to sync SystemConfig from AnythingLLM", e);
        }
    }
}
