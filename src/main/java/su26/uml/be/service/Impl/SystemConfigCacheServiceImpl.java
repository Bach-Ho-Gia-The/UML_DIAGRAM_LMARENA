package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import su26.uml.be.config.anythingllm.AnythingLlmClient;
import su26.uml.be.config.anythingllm.AnythingLlmProperties;
import su26.uml.be.service.SystemConfigCacheService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SystemConfigCacheServiceImpl implements SystemConfigCacheService {

    AnythingLlmClient anythingLlmClient;
    AnythingLlmProperties anythingLlmProperties;

    @Override
    public SystemConfig getActiveConfig() {
        try {
            Map<String, Object> raw = anythingLlmClient.getWorkspaceBySlug(anythingLlmProperties.workspaceSlug());
            Map<String, Object> workspace = extractWorkspace(raw);
            String modelName = str(workspace.get("chatModel"));
            String provider = str(workspace.get("chatProvider"));
            if (modelName == null) modelName = anythingLlmProperties.modelName();
            if (provider == null) provider = "unknown";
            return new SystemConfig(modelName, provider, anythingLlmProperties.workspaceSlug());
        } catch (Exception e) {
            log.error("Failed to fetch active config from AnythingLLM workspace", e);
            return new SystemConfig(anythingLlmProperties.modelName(), "unknown", anythingLlmProperties.workspaceSlug());
        }
    }

    private Map<String, Object> extractWorkspace(Map<String, Object> raw) {
        if (raw == null) return Map.of();
        if (raw.containsKey("workspace")) {
            Object ws = raw.get("workspace");
            if (ws instanceof List && !((List<?>) ws).isEmpty()) {
                @SuppressWarnings("unchecked")
                var map = (Map<String, Object>) ((List<?>) ws).get(0);
                return map;
            } else if (ws instanceof Map) {
                @SuppressWarnings("unchecked")
                var map = (Map<String, Object>) ws;
                return map;
            }
        }
        return raw;
    }

    private String str(Object value) {
        return value != null ? value.toString() : null;
    }
}
