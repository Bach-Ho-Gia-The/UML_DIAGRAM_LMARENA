package su26.uml.be.service.scheduleJobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import su26.uml.be.service.SubscriptionService;

/**
 * Hạ gói khi hết kỳ (Chặng 3.1 + Cancel): sub ACTIVE quá endDate → EXPIRED + về base.
 * Chạy mỗi giờ — entitlement đã chính xác từng giây qua filter endDate; job chỉ dọn state DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryJob {

    private final SubscriptionService subscriptionService;

    @Scheduled(fixedDelay = 3_600_000) // mỗi 1 giờ
    @SchedulerLock(name = "subscriptionExpiry", lockAtMostFor = "55m", lockAtLeastFor = "1m")
    public void expire() {
        try {
            subscriptionService.expireEndedSubscriptions();
        } catch (Exception e) {
            log.error("SubscriptionExpiryJob failed", e);
        }
    }
}
