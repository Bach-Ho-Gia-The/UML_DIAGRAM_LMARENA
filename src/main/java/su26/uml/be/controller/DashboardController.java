package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.DashboardOverviewResponse;
import su26.uml.be.dto.response.DashboardStatResponse;
import su26.uml.be.dto.response.RevenueTrendEntry;
import su26.uml.be.service.DashboardService;

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
}
