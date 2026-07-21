package su26.uml.be.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import su26.uml.be.dto.response.OwnerGroupResponse;
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

    // Biến thể phân trang — KHÔNG dùng @EntityGraph(sheets) vì fetch collection kèm Pageable
    // khiến Hibernate phân trang trong bộ nhớ (HHH000104); diagramCount lazy-load trong tx read-only.
    Page<Project> findAllByUserAndIsDeletedFalse(User user, Pageable pageable);
    Page<Project> findAllByUserAndIsDeletedFalseAndIsDraftFalse(User user, Pageable pageable);
    Page<Project> findAllByUserAndIsDeletedFalseAndIsDraftTrue(User user, Pageable pageable);
    Page<Project> findAllByIsDeletedFalseAndIsDraftFalse(Pageable pageable);

    long countByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime from, LocalDateTime to);

    // ─── Admin Projects: 3 nguồn dữ liệu độc lập (stats / owners / by-owner) ────────

    // Stats toàn bảng — độc lập phân trang
    long countByIsDeletedFalse();
    long countByIsDeletedFalseAndIsDraftTrue();
    long countByIsDeletedFalseAndIsDraftFalse();

    // Phân trang TẦNG NGOÀI: mỗi owner + số project của họ. GROUP BY nên phải khai báo countQuery
    // riêng (Spring Data không tự suy ra count cho query GROUP BY) = COUNT(DISTINCT owner).
    // KHÔNG truyền Sort qua Pageable (ORDER BY đã cố định trong query) để tránh xung đột.
    @Query(value = "select new su26.uml.be.dto.response.OwnerGroupResponse("
            + "p.user.id, p.user.fullName, p.user.email, count(p)) "
            + "from Project p where p.isDeleted = false "
            + "group by p.user.id, p.user.fullName, p.user.email "
            + "order by count(p) desc",
            countQuery = "select count(distinct p.user.id) from Project p where p.isDeleted = false")
    Page<OwnerGroupResponse> findOwnerGroups(Pageable pageable);

    // Phân trang TẦNG TRONG: project của riêng một owner
    Page<Project> findAllByUser_IdAndIsDeletedFalse(UUID ownerId, Pageable pageable);

    // ─── Lifecycle Phase 1 (Chặng 1B, additive — dùng cho Archive/capacity ở Chặng 4) ───
    /** Project của user theo lifecycleStatus (vd loại ARCHIVED_OVER_LIMIT khỏi list thường). */
    Page<Project> findAllByUserAndIsDeletedFalseAndLifecycleStatus(User user, String lifecycleStatus, Pageable pageable);
    List<Project> findByUser_IdAndLifecycleStatus(UUID userId, String lifecycleStatus);
    long countByUser_IdAndLifecycleStatus(UUID userId, String lifecycleStatus);
}
