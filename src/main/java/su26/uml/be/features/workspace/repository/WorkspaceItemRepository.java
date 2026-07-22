package su26.uml.be.features.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.project.entity.Project;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.features.workspace.entity.WorkspaceItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceItemRepository extends JpaRepository<WorkspaceItem, UUID> {

    List<WorkspaceItem> findAllByProjectOrderByOrderIndexAsc(Project project);

    void deleteAllByProject(Project project);

    Optional<WorkspaceItem> findBySheet(Sheet sheet);

    boolean existsBySheet(Sheet sheet);
}