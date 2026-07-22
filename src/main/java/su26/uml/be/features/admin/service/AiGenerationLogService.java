package su26.uml.be.features.admin.service;

import su26.uml.be.common.constant.enums.EstimationMethod;

public interface AiGenerationLogService {

    void log(String sessionId, String userId, int inputTokens, int outputTokens,
             EstimationMethod method, long latencyMs, boolean success, String errorMessage,
             String provider, String modelName);
}