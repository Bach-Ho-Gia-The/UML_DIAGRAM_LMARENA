package su26.uml.be.service;

import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.DashboardOverviewResponse;
import su26.uml.be.dto.response.DashboardStatResponse;
import su26.uml.be.dto.response.RevenueTrendEntry;
import su26.uml.be.dto.response.TopCostDriverResponse;
import su26.uml.be.dto.response.AiModelStatsResponse;
import su26.uml.be.dto.response.TopProjectResponse;

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
}
