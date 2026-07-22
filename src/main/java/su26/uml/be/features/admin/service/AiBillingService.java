package su26.uml.be.features.admin.service;

import su26.uml.be.common.constant.enums.EstimationMethod;

import java.math.BigDecimal;

public interface AiBillingService {

    BillingResult calculateCost(String provider, String modelName, int inputTokens, int outputTokens, EstimationMethod method);

    record BillingResult(BigDecimal costUsd, EstimationMethod finalMethod) {}
}