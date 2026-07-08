package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.AiGenerationLog;
import su26.uml.be.entity.ProviderRate;
import su26.uml.be.enums.EstimationMethod;
import su26.uml.be.repository.AiGenerationLogRepository;
import su26.uml.be.service.ProviderRateCacheService;
import su26.uml.be.service.SystemConfigCacheService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AiBillingEventPublisher {

    AiGenerationLogRepository aiGenerationLogRepository;
    ProviderRateCacheService providerRateCacheService;
    SystemConfigCacheService systemConfigCacheService;

    @Async
    public void publishBill(
            String sessionId,
            String userId,
            int inputTokens,
            int outputTokens,
            EstimationMethod method,
            long latencyMs,
            boolean success,
            String errorMessage
    ) {
        try {
            // Đọc model + provider từ SystemConfig cache (source of truth)
            SystemConfigCacheService.SystemConfig config = systemConfigCacheService.getActiveModel();
            String modelName = config.modelName();
            String provider = config.provider();

            // Tra cứu rate từ Redis cache (hoặc DB nếu cache miss)
            Optional<ProviderRate> rateOpt = providerRateCacheService.getRate(provider, modelName);

            BigDecimal costUsd;
            EstimationMethod finalMethod = method;

            if (rateOpt.isPresent() && method == EstimationMethod.PROVIDER) {
                // Primary: tính cost từ rate cache
                ProviderRate rate = rateOpt.get();
                BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                        .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                        .multiply(rate.getRateInPer1k());
                BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                        .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                        .multiply(rate.getRateOutPer1k());
                costUsd = inputCost.add(outputCost).setScale(12, RoundingMode.HALF_UP);
            } else {
                // Fallback: rate not found → dùng ước lượng
                costUsd = estimateFallbackCost(inputTokens, outputTokens, method);
                finalMethod = method == EstimationMethod.PROVIDER ? EstimationMethod.UNKNOWN : method;
            }

            AiGenerationLog logEntry = AiGenerationLog.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .modelName(modelName)
                    .provider(provider)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(inputTokens + outputTokens)
                    .costUsd(costUsd)
                    .estimationMethod(finalMethod)
                    .latencyMs(latencyMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();

            aiGenerationLogRepository.save(logEntry);
            log.debug("AI billing log saved: model={}, tokens={}+{}, cost=${}, method={}",
                    modelName, inputTokens, outputTokens, costUsd, finalMethod);

        } catch (Exception e) {
            log.error("Failed to save AI billing log", e);
        }
    }

    /**
     * Fallback cost estimation khi không có provider rate.
     * Dùng mức mặc định: $0.002/1K input, $0.008/1K output (OpenAI GPT-4o-mini).
     */
    private BigDecimal estimateFallbackCost(int inputTokens, int outputTokens, EstimationMethod method) {
        BigDecimal rateIn = BigDecimal.valueOf(0.002);
        BigDecimal rateOut = BigDecimal.valueOf(0.008);
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                .multiply(rateIn);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                .multiply(rateOut);
        return inputCost.add(outputCost).setScale(12, RoundingMode.HALF_UP);
    }
}
