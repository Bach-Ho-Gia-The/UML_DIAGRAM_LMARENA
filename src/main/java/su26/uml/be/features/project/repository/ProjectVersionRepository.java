package su26.uml.be.features.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.project.entity.Project;
import su26.uml.be.features.project.entity.ProjectVersion;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectVersionRepository extends JpaRepository<ProjectVersion, UUID> {
    Optional<ProjectVersion> findFirstByProjectOrderByVersionNumberDesc(Project project);

    void deleteAllByProject(Project project);
}