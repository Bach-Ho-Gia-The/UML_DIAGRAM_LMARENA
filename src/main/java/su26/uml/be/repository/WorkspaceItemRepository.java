package su26.uml.be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.Sheet;
import su26.uml.be.entity.WorkspaceItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceItemRepository extends JpaRepository<WorkspaceItem, UUID> {

    List<WorkspaceItem> findAllByProjectOrderByOrderIndexAsc(Project project);

    Optional<WorkspaceItem> findBySheet(Sheet sheet);

    boolean existsBySheet(Sheet sheet);
}
