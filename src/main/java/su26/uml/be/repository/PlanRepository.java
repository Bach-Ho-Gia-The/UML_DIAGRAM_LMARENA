package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.Plan;
import su26.uml.be.enums.PlanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByStatusOrderByPriceAsc(PlanStatus status);

    boolean existsByNameIgnoreCase(String name);

    /** Gói mặc định cho user chưa có subscription = gói ACTIVE giá thấp nhất. */
    Optional<Plan> findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus status);

    /** Gói base = gói được đánh dấu isBasePlan = true và đang ACTIVE. */
    Optional<Plan> findFirstByIsBasePlanTrueAndStatus(PlanStatus status);

    /** Kiểm tra unique isBasePlan (chỉ 1 gói base duy nhất). */
    Optional<Plan> findByIsBasePlanTrue();

    /** Kiểm tra unique tierOrder. */
    boolean existsByTierOrder(Integer tierOrder);

    /** Ngưỡng rate-limit cao nhất (cho admin / user chưa có gói). */
    @Query("SELECT MAX(p.rateLimitPer10s) FROM Plan p")
    Integer findMaxRatePer10s();

    @Query("SELECT MAX(p.rateLimitPerMin) FROM Plan p")
    Integer findMaxRatePerMin();
}
