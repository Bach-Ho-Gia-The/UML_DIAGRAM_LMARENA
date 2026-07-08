package su26.uml.be.service;

public interface SystemConfigCacheService {

    SystemConfig getActiveModel();

    void sync();

    record SystemConfig(String modelName, String provider, String workspaceModelName) {}
}
