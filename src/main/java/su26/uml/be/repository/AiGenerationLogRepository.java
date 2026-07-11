package su26.uml.be.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import su26.uml.be.dto.projection.TopCostDriverProjection;
import su26.uml.be.entity.AiGenerationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtBetweenAndSuccess(LocalDateTime from, LocalDateTime to, boolean success);

    List<AiGenerationLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<AiGenerationLog> findByCreatedAtBetweenAndSuccessAndProviderAndModelNameOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, boolean success, String provider, String modelName);

    @Query("SELECT l.userId AS userId, SUM(l.costUsd) AS totalCost, " +
            "COUNT(l) AS requestCount, COALESCE(SUM(l.totalTokens), 0) AS totalTokens " +
            "FROM AiGenerationLog l WHERE l.userId IS NOT NULL " +
            "GROUP BY l.userId ORDER BY COUNT(l) DESC")
    List<TopCostDriverProjection> findTopCostDrivers(Pageable pageable);
}
