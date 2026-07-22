package su26.uml.be.features.diagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.diagram.entity.DiagramVersion;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.common.constant.enums.DiagramVersionSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagramVersionRepository extends JpaRepository<DiagramVersion, UUID> {

    List<DiagramVersion> findAllBySheetOrderByVersionNumberDesc(Sheet sheet);

    void deleteAllBySheetIn(Collection<Sheet> sheets);

    Optional<DiagramVersion> findFirstBySheetOrderByVersionNumberDesc(Sheet sheet);

    Optional<DiagramVersion> findFirstBySheetAndContentHashOrderByVersionNumberDesc(Sheet sheet, String contentHash);

    // Retention: chỉ AUTO bị prune, giữ nguyên MANUAL và các bản ghi audit restore
    List<DiagramVersion> findAllBySheetAndSourceOrderByVersionNumberAsc(Sheet sheet, DiagramVersionSource source);

    long countBySheetAndSource(Sheet sheet, DiagramVersionSource source);
}