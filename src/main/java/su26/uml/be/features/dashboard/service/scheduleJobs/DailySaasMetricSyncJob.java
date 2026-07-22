package su26.uml.be.features.dashboard.service.scheduleJobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import su26.uml.be.features.dashboard.service.SaasMetricSyncService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailySaasMetricSyncJob {

    private final SaasMetricSyncService syncService;

    @Scheduled(fixedDelay = 900_000)
    @SchedulerLock(name = "dailySaasMetricSync", lockAtMostFor = "14m", lockAtLeastFor = "2m")
    public void syncMetrics() {
        try {
            syncService.syncMetrics();
        } catch (Exception e) {
            log.error("DailySaasMetricSyncJob failed", e);
        }
    }
}