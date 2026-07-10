package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.Subscription;
import su26.uml.be.enums.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByStatus(SubscriptionStatus status);
    long countByStatusAndEndDateBetween(SubscriptionStatus status, LocalDateTime from, LocalDateTime to);
    long countByStartDateBeforeAndStatus(LocalDateTime before, SubscriptionStatus status);
    long countByPlanAndStatus(Plan plan, SubscriptionStatus status);
    boolean existsByPlanAndStatus(Plan plan, SubscriptionStatus status);
}
