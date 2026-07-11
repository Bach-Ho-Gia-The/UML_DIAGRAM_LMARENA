package su26.uml.be.service.adminDashboard.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.ProviderRate;
import su26.uml.be.enums.EstimationMethod;
import su26.uml.be.service.ProviderRateCacheService;
import su26.uml.be.service.adminDashboard.AiBillingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiBillingServiceImpl implements AiBillingService {

    ProviderRateCacheService providerRateCacheService;

    @Override
    public BillingResult calculateCost(String provider, String modelName, int inputTokens, int outputTokens, EstimationMethod method) {
        Optional<ProviderRate> rateOpt = providerRateCacheService.getRate(provider, modelName);

        BigDecimal costUsd;
        EstimationMethod finalMethod = method;

        if (rateOpt.isPresent() && method == EstimationMethod.PROVIDER) {
            ProviderRate rate = rateOpt.get();
            BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                    .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                    .multiply(rate.getRateInPer1k());
            BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                    .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                    .multiply(rate.getRateOutPer1k());
            costUsd = inputCost.add(outputCost).setScale(12, RoundingMode.HALF_UP);
        } else {
            costUsd = estimateFallbackCost(inputTokens, outputTokens, method);
            finalMethod = method == EstimationMethod.PROVIDER ? EstimationMethod.UNKNOWN : method;
        }

        return new BillingResult(costUsd, finalMethod);
    }

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
