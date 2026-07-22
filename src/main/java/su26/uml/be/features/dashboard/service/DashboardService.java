package su26.uml.be.features.dashboard.service;

import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.dashboard.dto.DashboardOverviewResponse;
import su26.uml.be.features.dashboard.dto.DashboardStatResponse;
import su26.uml.be.features.dashboard.dto.RevenueTrendEntry;
import su26.uml.be.features.dashboard.dto.TopCostDriverResponse;
import su26.uml.be.features.ai.dto.AiErrorLogEntry;
import su26.uml.be.features.ai.dto.AiModelStatsResponse;
import su26.uml.be.features.dashboard.dto.TopProjectResponse;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    ApiResponse<DashboardStatResponse> getUserStats(String range, LocalDate from, LocalDate to);
    ApiResponse<DashboardStatResponse> getProjectStats(String range, LocalDate from, LocalDate to);
    ApiResponse<DashboardStatResponse> getDiagramStats(String range, LocalDate from, LocalDate to);
    ApiResponse<DashboardOverviewResponse> getOverview(String range, LocalDate from, LocalDate to);
    ApiResponse<List<RevenueTrendEntry>> getRevenueTrend();
    ApiResponse<List<TopCostDriverResponse>> getTopCostDrivers(int limit);
    ApiResponse<List<TopProjectResponse>> getTopProjects(int limit);

    ApiResponse<List<AiModelStatsResponse>> getAiModelStats(String range, LocalDate from, LocalDate to);

    ApiResponse<List<AiErrorLogEntry>> getAiErrorLogs(String provider, String modelName, int limit);
}