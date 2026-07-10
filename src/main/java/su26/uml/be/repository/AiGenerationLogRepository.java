package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.AiGenerationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtBetweenAndSuccess(LocalDateTime from, LocalDateTime to, boolean success);

    List<AiGenerationLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
