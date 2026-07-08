package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.AiGenerationLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    @Query("SELECT COALESCE(SUM(l.costUsd), 0) FROM AiGenerationLog l WHERE l.createdAt BETWEEN :from AND :to AND l.success = true")
    BigDecimal sumCostUsdByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtBetweenAndSuccess(LocalDateTime from, LocalDateTime to, boolean success);

    @Query("SELECT COALESCE(AVG(l.latencyMs), 0) FROM AiGenerationLog l WHERE l.createdAt BETWEEN :from AND :to AND l.success = true")
    double avgLatencyMsByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(l.totalTokens), 0) FROM AiGenerationLog l WHERE l.createdAt BETWEEN :from AND :to AND l.success = true")
    long sumTotalTokensByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
