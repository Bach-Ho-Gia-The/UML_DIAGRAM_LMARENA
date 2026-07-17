package su26.uml.be.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Khóa row project để tuần tự hóa mọi mutation trên cây workspace của cùng một project
    // (chặn race 2 request move đồng thời tạo cycle mà check từng request không thấy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :projectId")
    Optional<Project> findWithLockById(@Param("projectId") UUID projectId);

    long countByUserAndIsDeletedFalse(User user);
    @EntityGraph(attributePaths = {"sheets"})
    List<Project> findAllByUserAndIsDeletedFalse(User user);

    @EntityGraph(attributePaths = {"sheets"})
    List<Project> findAllByUserAndIsDeletedFalseAndIsDraftFalse(User user);

    @EntityGraph(attributePaths = {"sheets"})
    List<Project> findAllByUserAndIsDeletedFalseAndIsDraftTrue(User user);

    List<Project> findAllByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = {"sheets"})
    List<Project> findAllByIsDeletedFalse();

    @EntityGraph(attributePaths = {"sheets"})
    List<Project> findAllByIsDeletedFalseAndIsDraftFalse();

    @EntityGraph(attributePaths = {"sheets"})
    List<Project> findAllByIsDeletedFalseAndIsDraftTrue();

    long countByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime from, LocalDateTime to);
}
