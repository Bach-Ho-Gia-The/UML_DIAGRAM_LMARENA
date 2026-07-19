package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.DiagramVersion;
import su26.uml.be.entity.Sheet;
import su26.uml.be.enums.DiagramVersionSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagramVersionRepository extends JpaRepository<DiagramVersion, UUID> {

    List<DiagramVersion> findAllBySheetOrderByVersionNumberDesc(Sheet sheet);

    // Xóa toàn bộ version của các sheet trong 1 câu DELETE — chạy TRƯỚC khi xóa sheet
    // (FK diagram_versions.sheet_id NOT NULL, không cascade). Bulk statement an toàn với
    // self-FK restored_from_version_id vì các bản ghi tham chiếu nhau cùng bị xóa trong cùng statement.
    @Modifying
    @Query("delete from DiagramVersion dv where dv.sheet in :sheets")
    void deleteAllBySheetIn(@Param("sheets") Collection<Sheet> sheets);

    Optional<DiagramVersion> findFirstBySheetOrderByVersionNumberDesc(Sheet sheet);

    Optional<DiagramVersion> findFirstBySheetAndContentHashOrderByVersionNumberDesc(Sheet sheet, String contentHash);

    // Retention: chỉ AUTO bị prune, giữ nguyên MANUAL và các bản ghi audit restore
    List<DiagramVersion> findAllBySheetAndSourceOrderByVersionNumberAsc(Sheet sheet, DiagramVersionSource source);

    long countBySheetAndSource(Sheet sheet, DiagramVersionSource source);
}
