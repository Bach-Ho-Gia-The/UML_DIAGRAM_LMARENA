package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.EntitlementGracePeriod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho kỳ ân hạn reconciliation (Chặng 4). Additive — chưa code nào gọi tới Chặng 4A.
 * PK là UUID (kế thừa từ BaseEntity).
 */
@Repository
public interface EntitlementGracePeriodRepository extends JpaRepository<EntitlementGracePeriod, UUID> {

    /** Kỳ ân hạn hiện tại của user theo trạng thái (vd status = "ACTIVE"). */
    Optional<EntitlementGracePeriod> findByUserIdAndStatus(UUID userId, String status);

    /** Kỳ ân hạn quá hạn (endsAt < now) cần job xử lý archive (vd status = "ACTIVE"). */
    List<EntitlementGracePeriod> findByStatusAndEndsAtBefore(String status, LocalDateTime now);
}
