package su26.uml.be.features.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.dashboard.dto.DashboardOverviewResponse;
import su26.uml.be.features.dashboard.dto.DashboardStatResponse;
import su26.uml.be.features.dashboard.dto.RevenueTrendEntry;
import su26.uml.be.features.dashboard.dto.TopCostDriverResponse;
import su26.uml.be.features.ai.dto.AiErrorLogEntry;
import su26.uml.be.features.ai.dto.AiModelStatsResponse;
import su26.uml.be.features.dashboard.dto.TopProjectResponse;
import su26.uml.be.features.dashboard.service.DashboardService;

import java.util.List;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Dashboard", description = "Admin dashboard aggregate data (users, projects, diagrams, AI latency).")
public class DashboardController {

    DashboardService dashboardService;

    @GetMapping("/users")
    @Operation(summary = "User registration stats",
            description = "Returns total user count, delta vs previous period, trend direction, and a 12-point sparkline. " +
                    "Params: range=24h|7d|30d|custom (default 30d). When range=custom, from and to are required.")
    public ApiResponse<DashboardStatResponse> getUserStats(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getUserStats(range, from, to);
    }

    @GetMapping("/projects")
    @Operation(summary = "Project creation stats",
            description = "Returns total active project count, delta vs previous period, trend, and 12-point sparkline. " +
                    "Params: range=24h|7d|30d|custom (default 30d). Deleted projects are excluded.")
    public ApiResponse<DashboardStatResponse> getProjectStats(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getProjectStats(range, from, to);
    }

    @GetMapping("/diagrams")
    @Operation(summary = "Diagram (sheet) creation stats",
            description = "Returns total diagram count, delta vs previous period, trend, and 12-point sparkline. " +
                    "Params: range=24h|7d|30d|custom (default 30d).")
    public ApiResponse<DashboardStatResponse> getDiagramStats(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getDiagramStats(range, from, to);
    }

    @GetMapping("/overview")
    @Operation(summary = "Aggregated SaaS overview",
            description = "Returns user metrics (DAU/MAU/total), revenue metrics (MRR/churn/ARPU/margin), " +
                    "and AI metrics (requests/cost/latency/error/tokens). " +
                    "Params range/from/to control the AI metric window (user/revenue are always current).")
    public ApiResponse<DashboardOverviewResponse> getOverview(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getOverview(range, from, to);
    }

    @GetMapping("/revenue-trend")
    @Operation(summary = "MRR trend over time",
            description = "Returns daily MRR snapshots from DailySaasMetric for the revenue trend chart.")
    public ApiResponse<List<RevenueTrendEntry>> getRevenueTrend() {
        return dashboardService.getRevenueTrend();
    }

    @GetMapping("/top-cost-drivers")
    @Operation(summary = "Top users by AI usage",
            description = "Returns the top users ranked by AI request count (desc) from ai_generation_logs, " +
                    "enriched with full name, email and total tokens. Param: limit (default 5, max 50).")
    public ApiResponse<List<TopCostDriverResponse>> getTopCostDrivers(
            @RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopCostDrivers(limit);
    }

    @GetMapping("/ai-model-stats")
    @Operation(summary = "AI error & usage stats by provider/model",
            description = "Returns per-provider-model stats: requests, errors, error rate (%), total cost. " +
                    "Params: range=24h|7d|30d|custom (default 30d).")
    public ApiResponse<List<AiModelStatsResponse>> getAiModelStats(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.getAiModelStats(range, from, to);
    }

    @GetMapping("/ai-error-logs")
    @Operation(summary = "AI error logs by provider/model",
            description = "Returns recent error logs (success=false) for a given provider and model, " +
                    "ordered by createdAt desc. Params: provider, modelName (required), limit (default 20, max 50).")
    public ApiResponse<List<AiErrorLogEntry>> getAiErrorLogs(
            @RequestParam String provider,
            @RequestParam String modelName,
            @RequestParam(defaultValue = "20") int limit) {
        return dashboardService.getAiErrorLogs(provider, modelName, limit);
    }

    @GetMapping("/top-projects")
    @Operation(summary = "Top projects by diagram count",
            description = "Returns the top non-deleted projects ranked by number of diagrams (sheets). " +
                    "Param: limit (default 5, max 50).")
    public ApiResponse<List<TopProjectResponse>> getTopProjects(
            @RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopProjects(limit);
    }
}