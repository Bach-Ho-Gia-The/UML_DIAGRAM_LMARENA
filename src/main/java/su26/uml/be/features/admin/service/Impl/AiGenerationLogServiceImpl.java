package su26.uml.be.features.admin.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import su26.uml.be.features.ai.entity.AiGenerationLog;
import su26.uml.be.common.constant.enums.EstimationMethod;
import su26.uml.be.features.ai.repository.AiGenerationLogRepository;
import su26.uml.be.features.admin.service.AiBillingService;
import su26.uml.be.features.admin.service.AiGenerationLogService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AiGenerationLogServiceImpl implements AiGenerationLogService {

    AiGenerationLogRepository aiGenerationLogRepository;
    AiBillingService aiBillingService;

    @Async
    @Override
    public void log(String sessionId, String userId, int inputTokens, int outputTokens,
                    EstimationMethod method, long latencyMs, boolean success, String errorMessage,
                    String provider, String modelName) {
        try {
            AiBillingService.BillingResult billing = aiBillingService.calculateCost(
                    provider, modelName, inputTokens, outputTokens, method);

            AiGenerationLog logEntry = AiGenerationLog.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .modelName(modelName)
                    .provider(provider)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(inputTokens + outputTokens)
                    .costUsd(billing.costUsd())
                    .estimationMethod(billing.finalMethod())
                    .latencyMs(latencyMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();

            aiGenerationLogRepository.save(logEntry);
            log.debug("AI billing log saved: model={}, tokens={}+{}, cost=${}, method={}",
                    modelName, inputTokens, outputTokens, billing.costUsd(), billing.finalMethod());

        } catch (Exception e) {
            log.error("Failed to save AI billing log", e);
        }
    }
}