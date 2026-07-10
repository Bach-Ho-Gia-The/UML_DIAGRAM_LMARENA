package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.DailySaasMetric;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailySaasMetricRepository extends JpaRepository<DailySaasMetric, UUID> {

    Optional<DailySaasMetric> findTopByOrderBySnapshotDateDesc();

    List<DailySaasMetric> findAllByOrderBySnapshotDateAsc();
}
