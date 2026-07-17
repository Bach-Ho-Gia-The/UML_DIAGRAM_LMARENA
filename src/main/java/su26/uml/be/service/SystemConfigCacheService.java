package su26.uml.be.service;

public interface SystemConfigCacheService {

    SystemConfig getActiveConfig();

    record SystemConfig(String modelName, String provider, String workspaceModelName) {}
}
