package su26.uml.be.service.adminDashboard;

import su26.uml.be.enums.EstimationMethod;

public interface AiGenerationLogService {

    void log(String sessionId, String userId, int inputTokens, int outputTokens,
             EstimationMethod method, long latencyMs, boolean success, String errorMessage);
}
