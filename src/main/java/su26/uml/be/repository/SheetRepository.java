package su26.uml.be.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import su26.uml.be.dto.projection.TopProjectProjection;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.Sheet;
import su26.uml.be.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SheetRepository extends JpaRepository<Sheet, UUID> {
    List<Sheet> findAllByProjectOrderByOrderIndexAsc(Project project);

    void deleteAllByProject(Project project);

    long countByProject_UserAndProject_IsDeletedFalse(User user);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT s.project.id AS projectId, s.project.projectName AS projectName, " +
            "s.project.user.email AS ownerEmail, COUNT(s) AS diagramCount " +
            "FROM Sheet s WHERE s.project.isDeleted = false " +
            "GROUP BY s.project.id, s.project.projectName, s.project.user.email " +
            "ORDER BY COUNT(s) DESC")
    List<TopProjectProjection> findTopProjects(Pageable pageable);
}
