package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.PaymentTransaction;
import su26.uml.be.enums.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByOrderCode(Long orderCode);
    Optional<PaymentTransaction> findByOrderCodeAndStatus(Long orderCode, PaymentStatus status);

    // ─── Subscription Phase 1 (Chặng 2A) ───
    /**
     * Chuyển PENDING→PAID atomic. Trả về số dòng đổi: 1 = ta là người đầu tiên xử lý (cấp quyền);
     * 0 = đã xử lý hoặc không PENDING → bỏ qua. Đây là chốt idempotent chống double-grant (R3).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PaymentTransaction p SET p.status = :paid WHERE p.orderCode = :orderCode AND p.status = :pending")
    int markPaidIfPending(@Param("orderCode") Long orderCode,
                          @Param("paid") PaymentStatus paid,
                          @Param("pending") PaymentStatus pending);

    /** Chặn tạo payment mới khi đang có giao dịch chờ (BR PENDING_PAYMENT_EXISTS). */
    boolean existsByUser_IdAndStatus(UUID userId, PaymentStatus status);
}
