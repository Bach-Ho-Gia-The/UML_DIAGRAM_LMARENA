package su26.uml.be.features.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.dashboard.entity.DailySaasMetric;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailySaasMetricRepository extends JpaRepository<DailySaasMetric, UUID> {

    Optional<DailySaasMetric> findTopByOrderBySnapshotDateDesc();

    Optional<DailySaasMetric> findBySnapshotDate(LocalDate snapshotDate);

    List<DailySaasMetric> findAllByOrderBySnapshotDateAsc();
}