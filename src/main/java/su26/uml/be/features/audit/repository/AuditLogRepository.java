package su26.uml.be.features.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import su26.uml.be.features.audit.entity.AuditLog;

import java.util.List;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /** Danh sách các action đang tồn tại trong DB — dùng đổ dropdown lọc động (không hardcode). */
    @Query("select distinct a.action from AuditLog a order by a.action asc")
    List<String> findDistinctActions();
}