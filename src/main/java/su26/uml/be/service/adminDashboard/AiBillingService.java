package su26.uml.be.service.adminDashboard;

import su26.uml.be.enums.EstimationMethod;

import java.math.BigDecimal;

public interface AiBillingService {

    BillingResult calculateCost(String provider, String modelName, int inputTokens, int outputTokens, EstimationMethod method);

    record BillingResult(BigDecimal costUsd, EstimationMethod finalMethod) {}
}
