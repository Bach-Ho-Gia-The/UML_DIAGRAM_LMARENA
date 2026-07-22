package su26.uml.be.features.ai.service;

public interface SystemConfigCacheService {

    SystemConfig getActiveConfig();

    record SystemConfig(String modelName, String provider, String workspaceModelName) {}
}