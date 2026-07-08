package su26.uml.be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import su26.uml.be.entity.Notification;
import su26.uml.be.enums.NotificationSeverity;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findByIsReadOrderByCreatedAtDesc(boolean isRead, Pageable pageable);

    long countByTypeAndSeverityAndCreatedAtAfter(String type, NotificationSeverity severity, LocalDateTime after);
}
