package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByStatus(SubscriptionStatus status);
    long countByStatusAndEndDateBetween(SubscriptionStatus status, LocalDateTime from, LocalDateTime to);
    long countByStartDateBeforeAndStatus(LocalDateTime before, SubscriptionStatus status);
    long countByPlanAndStatus(Plan plan, SubscriptionStatus status);
    boolean existsByPlanAndStatus(Plan plan, SubscriptionStatus status);

    /** Gói ACTIVE hiện tại của user (mới nhất theo endDate), chỉ lấy sub chưa hết hạn. */
    Optional<Subscription> findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(UUID userId, SubscriptionStatus status, LocalDateTime dateTime);

    /** Gói ACTIVE hiện tại của user (mới nhất theo endDate). */
    Optional<Subscription> findFirstByUser_IdAndStatusOrderByEndDateDesc(UUID userId, SubscriptionStatus status);

    // ─── Subscription Phase 1 (Chặng 1B, additive — chưa code nào gọi tới Chặng 1C/2) ───
    /**
     * Paid subscription "effective" theo thiết kế §1.3: status ∈ {statuses}, startDate ≤ now, endDate > now.
     * (Chữ ký lệch plan Task 1.8 để hợp lệ Spring Data: User_Id + enum SubscriptionStatus + LessThanEqual.)
     */
    Optional<Subscription> findFirstByUser_IdAndStatusInAndStartDateLessThanEqualAndEndDateAfterOrderByEndDateDesc(
            UUID userId, List<SubscriptionStatus> statuses, LocalDateTime start, LocalDateTime end);
}
