package su26.uml.be.features.dashboard.dto;

import java.math.BigDecimal;

/**
 * Aggregate row for "Top Cost Drivers": one user with their summed AI cost.
 * Populated by {@code AiGenerationLogRepository.findTopCostDrivers(...)}.
 */
public interface TopCostDriverProjection {
    String getUserId();
    BigDecimal getTotalCost();
    long getRequestCount();
    long getTotalTokens();
}