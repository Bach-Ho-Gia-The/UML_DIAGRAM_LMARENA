package su26.uml.be.features.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.plan.entity.FeatureCatalog;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeatureCatalogRepository extends JpaRepository<FeatureCatalog, UUID> {
    List<FeatureCatalog> findAllByOrderBySortOrderAscLabelAsc();

    boolean existsByLabelIgnoreCase(String label);
}