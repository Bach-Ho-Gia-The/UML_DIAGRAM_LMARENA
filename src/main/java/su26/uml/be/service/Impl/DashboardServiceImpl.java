package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.dto.projection.TopCostDriverProjection;
import su26.uml.be.dto.projection.TopProjectProjection;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.DashboardOverviewResponse;
import su26.uml.be.dto.response.DashboardStatResponse;
import su26.uml.be.dto.response.RevenueTrendEntry;
import su26.uml.be.dto.response.TopCostDriverResponse;
import su26.uml.be.dto.response.TopProjectResponse;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.enums.UserStatus;
import su26.uml.be.entity.AiGenerationLog;
import su26.uml.be.entity.User;
import su26.uml.be.repository.AiGenerationLogRepository;
import su26.uml.be.repository.DailySaasMetricRepository;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.repository.SheetRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.CoreActivityTracker;
import su26.uml.be.service.DashboardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    UserRepository userRepository;
    ProjectRepository projectRepository;
    SheetRepository sheetRepository;
    SubscriptionRepository subscriptionRepository;
    CoreActivityTracker activityTracker;
    DailySaasMetricRepository metricRepository;
    AiGenerationLogRepository aiGenerationLogRepository;

    @Override
    public ApiResponse<DashboardStatResponse> getUserStats(String range, LocalDate from, LocalDate to) {
        return buildStat(range, from, to,
                (f, t) -> userRepository.countByCreatedAtBetweenAndStatusAndRoleRoleName(f, t, UserStatus.ACTIVE, "USER"));
    }

    @Override
    public ApiResponse<DashboardStatResponse> getProjectStats(String range, LocalDate from, LocalDate to) {
        return buildStat(range, from, to,
                projectRepository::countByCreatedAtBetweenAndIsDeletedFalse);
    }

    @Override
    public ApiResponse<DashboardStatResponse> getDiagramStats(String range, LocalDate from, LocalDate to) {
        return buildStat(range, from, to,
                sheetRepository::countByCreatedAtBetween);
    }

    @Override
    public ApiResponse<DashboardOverviewResponse> getOverview(String range, LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last30d = now.minusDays(30);

        long total = userRepository.countByStatusAndRoleRoleName(UserStatus.ACTIVE, "USER");
        long dau = activityTracker.getDailyActiveUsers();

        long mau = metricRepository.findTopByOrderBySnapshotDateDesc()
                .map(s -> s.getMau())
                .orElse(0L);

        BigDecimal mrr = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).stream()
                .map(s -> s.getPlan().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeBefore30d = subscriptionRepository.countByStartDateBeforeAndStatus(last30d, SubscriptionStatus.ACTIVE);
        long churned30d = subscriptionRepository.countByStatusAndEndDateBetween(SubscriptionStatus.EXPIRED, last30d, now)
                + subscriptionRepository.countByStatusAndEndDateBetween(SubscriptionStatus.CANCELLED, last30d, now);
        double churnRate = activeBefore30d == 0 ? 0 : Math.round((double) churned30d / activeBefore30d * 100 * 10.0) / 10.0;

        BigDecimal arpu = total == 0 ? BigDecimal.ZERO
                : mrr.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        LocalDateTime aiFrom = resolveAiRange(range, from, to, now);
        List<AiGenerationLog> aiLogs = aiGenerationLogRepository.findByCreatedAtBetween(aiFrom, now);
        long aiRequests = aiLogs.size();
        long aiTotalTokens = aiLogs.stream().mapToInt(AiGenerationLog::getTotalTokens).sum();
        BigDecimal aiCost = aiLogs.stream().map(AiGenerationLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long aiErrors = aiLogs.stream().filter(l -> !l.isSuccess()).count();
        double aiErrorRate = aiRequests == 0 ? 0 : Math.round((double) aiErrors / aiRequests * 100 * 10.0) / 10.0;
        double aiAvgLatency = aiLogs.stream().mapToLong(AiGenerationLog::getLatencyMs).average().orElse(0);

        DashboardOverviewResponse overview = DashboardOverviewResponse.builder()
                .users(DashboardOverviewResponse.UserMetrics.builder()
                        .total(total).dau(dau).mau(mau).build())
                .revenue(DashboardOverviewResponse.RevenueMetrics.builder()
                        .mrr(mrr).churnRate(churnRate).arpu(arpu).margin(mrr).build())
                .ai(DashboardOverviewResponse.AiMetrics.builder()
                        .requests(aiRequests).costUsd(aiCost).errorRate(aiErrorRate)
                        .avgLatencyMs(aiAvgLatency).totalTokens(aiTotalTokens).build())
                .build();

        return ApiResponse.success("OK", overview);
    }

    private LocalDateTime resolveAiRange(String range, LocalDate from, LocalDate to, LocalDateTime now) {
        return switch (range) {
            case "24h" -> now.minusHours(24);
            case "7d" -> now.minusDays(7);
            case "custom" -> {
                if (from == null || to == null) yield now.minusDays(30);
                yield from.atStartOfDay();
            }
            default -> now.minusDays(30);
        };
    }

    @Override
    public ApiResponse<List<TopCostDriverResponse>> getTopCostDrivers(int limit) {
        List<TopCostDriverProjection> rows = aiGenerationLogRepository.findTopCostDrivers(PageRequest.of(0, clampLimit(limit)));

        List<UUID> userIds = rows.stream()
                .map(r -> parseUuid(r.getUserId()))
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TopCostDriverResponse> result = rows.stream()
                .map(r -> {
                    UUID uid = parseUuid(r.getUserId());
                    User u = uid == null ? null : userMap.get(uid);
                    return TopCostDriverResponse.builder()
                            .userId(r.getUserId())
                            .fullName(u != null ? u.getFullName() : "Unknown")
                            .email(u != null ? u.getEmail() : null)
                            .requestCount(r.getRequestCount())
                            .totalTokens(r.getTotalTokens())
                            .totalCostUsd(r.getTotalCost() != null ? r.getTotalCost() : BigDecimal.ZERO)
                            .build();
                })
                .toList();

        return ApiResponse.success("OK", result);
    }

    @Override
    public ApiResponse<List<TopProjectResponse>> getTopProjects(int limit) {
        List<TopProjectResponse> result = sheetRepository.findTopProjects(PageRequest.of(0, clampLimit(limit))).stream()
                .map(r -> TopProjectResponse.builder()
                        .projectId(r.getProjectId().toString())
                        .projectName(r.getProjectName())
                        .ownerEmail(r.getOwnerEmail())
                        .diagramCount(r.getDiagramCount())
                        .build())
                .toList();

        return ApiResponse.success("OK", result);
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public ApiResponse<List<RevenueTrendEntry>> getRevenueTrend() {
        List<RevenueTrendEntry> entries = metricRepository.findAllByOrderBySnapshotDateAsc().stream()
                .map(m -> RevenueTrendEntry.builder().date(m.getSnapshotDate()).mrr(m.getMrr()).build())
                .collect(Collectors.toList());
        return ApiResponse.success("OK", entries);
    }

    private ApiResponse<DashboardStatResponse> buildStat(
            String range, LocalDate from, LocalDate to,
            BiFunction<LocalDateTime, LocalDateTime, Long> counter) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rangeFrom;
        LocalDateTime rangeTo;

        switch (range) {
            case "24h" -> {
                rangeTo = now;
                rangeFrom = now.minusHours(24);
            }
            case "7d" -> {
                rangeTo = now;
                rangeFrom = now.minusDays(7);
            }
            case "custom" -> {
                if (from == null || to == null) {
                    rangeTo = now;
                    rangeFrom = now.minusDays(30);
                } else {
                    rangeFrom = from.atStartOfDay();
                    rangeTo = to.atTime(LocalTime.MAX);
                }
            }
            default -> {
                rangeTo = now;
                rangeFrom = now.minusDays(30);
            }
        }

        long periodMillis = ChronoUnit.MILLIS.between(rangeFrom, rangeTo);
        LocalDateTime prevFrom = rangeFrom.minus(periodMillis, ChronoUnit.MILLIS);
        LocalDateTime prevTo = rangeFrom;

        long currentCount = counter.apply(rangeFrom, rangeTo);
        long prevCount = counter.apply(prevFrom, prevTo);

        double delta = prevCount == 0
                ? (currentCount > 0 ? 100 : 0)
                : ((double) (currentCount - prevCount) / prevCount) * 100;
        delta = Math.round(delta * 10.0) / 10.0;

        String trend = delta >= 0 ? "up" : "down";

        long segmentMillis = periodMillis / 12;
        List<Long> sparkline = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDateTime segFrom = rangeFrom.plus(i * segmentMillis, ChronoUnit.MILLIS);
            LocalDateTime segTo = rangeFrom.plus((i + 1) * segmentMillis, ChronoUnit.MILLIS);
            long segCount = counter.apply(segFrom, segTo);
            sparkline.add(segCount);
        }

        DashboardStatResponse result = DashboardStatResponse.builder()
                .total(currentCount)
                .delta(delta)
                .trend(trend)
                .sparkline(sparkline)
                .build();

        return ApiResponse.success("OK", result);
    }
}
