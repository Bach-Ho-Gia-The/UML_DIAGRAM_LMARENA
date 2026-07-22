package su26.uml.be.features.usage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.usage.entity.UserQuota;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserQuotaRepository extends JpaRepository<UserQuota, UUID> {

    Optional<UserQuota> findByUserId(UUID userId);

    // ─── Subscription Phase 1 (Chặng 1B, additive) ───
    /** Quota có kỳ hiện tại chưa hết (quotaPeriodEnd > now). Dùng cho QuotaPeriodService ở Chặng 1C. */
    Optional<UserQuota> findByUserIdAndQuotaPeriodEndAfter(UUID userId, java.time.LocalDateTime now);

    /**
     * Reserve atomic 1 AI request: chỉ trừ khi còn quota (hoặc unlimited).
     * @return số dòng bị đổi — 1 = reserve OK, 0 = hết quota (402).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE UserQuota q SET q.aiUsed = q.aiUsed + 1 "
            + "WHERE q.userId = :userId AND (q.aiLimit = -1 OR q.aiUsed < q.aiLimit)")
    int tryReserveAi(@Param("userId") UUID userId);

    /** Hoàn 1 AI request (rollback khi AI fail). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE UserQuota q SET q.aiUsed = q.aiUsed - 1 WHERE q.userId = :userId AND q.aiUsed > 0")
    int rollbackAi(@Param("userId") UUID userId);
}