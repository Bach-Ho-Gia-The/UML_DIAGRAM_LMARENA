package su26.uml.be.features.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.plan.entity.Plan;
import su26.uml.be.common.constant.enums.PlanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByStatusOrderByPriceAsc(PlanStatus status);

    boolean existsByNameIgnoreCase(String name);

    /**
     * Thứ tự xếp tier (D4 — một nguồn duy nhất): gói mặc định (isDefaultPlan=true) đứng đầu,
     * gói báo giá (contactSales=true) đứng cuối, giá tăng dần (NULLS LAST), cùng giá thì ai tạo
     * trước đứng trước. Kết quả: Free=0, Education=1, Standard=2, Pro=3, Enterprise=4.
     */
    @Query("SELECT p FROM Plan p WHERE p.status = :status ORDER BY "
            + "CASE WHEN p.isDefaultPlan = TRUE THEN 0 ELSE 1 END, "
            + "CASE WHEN p.contactSales = TRUE THEN 1 ELSE 0 END, "
            + "p.price ASC NULLS LAST, "
            + "p.createdAt ASC")
    List<Plan> findAllOrderedByTier(@Param("status") PlanStatus status);

    /** Gói mặc định đang ACTIVE — nguồn entitlements cho user chưa có subscription. */
    Optional<Plan> findFirstByStatusAndIsDefaultPlanTrue(PlanStatus status);

    /**
     * @deprecated Không còn call-site — logic "gói rẻ nhất" được thay bằng cờ isDefaultPlan (D4/D5).
     * Giữ lại chỉ để tương thích; dùng {@link #findFirstByStatusAndIsDefaultPlanTrue(PlanStatus)}.
     */
    @Deprecated
    Optional<Plan> findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus status);

    /** Có gói mặc định ACTIVE nào không (dùng khi CREATE). */
    boolean existsByStatusAndIsDefaultPlanTrue(PlanStatus status);

    /** Có gói mặc định ACTIVE KHÁC gói đang sửa không (dùng khi UPDATE — tránh lỗi id <> NULL). */
    boolean existsByStatusAndIsDefaultPlanTrueAndIdNot(PlanStatus status, UUID id);

    /** Đếm gói mặc định ACTIVE KHÁC gói đang sửa — phân biệt "đã có default" với "default cuối cùng". */
    long countByStatusAndIsDefaultPlanTrueAndIdNot(PlanStatus status, UUID id);

    /** Ngưỡng rate-limit cao nhất (cho admin / user chưa có gói). */
    @Query("SELECT MAX(p.rateLimitPer10s) FROM Plan p")
    Integer findMaxRatePer10s();

    @Query("SELECT MAX(p.rateLimitPerMin) FROM Plan p")
    Integer findMaxRatePerMin();
}
