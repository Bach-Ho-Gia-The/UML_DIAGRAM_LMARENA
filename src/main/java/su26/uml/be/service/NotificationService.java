package su26.uml.be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import su26.uml.be.dto.response.NotificationResponse;
import su26.uml.be.enums.NotificationSeverity;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(String type, String title, String message, NotificationSeverity severity);

    Page<NotificationResponse> getNotifications(Pageable pageable, Boolean read);

    NotificationResponse getNotification(UUID id);

    void markRead(UUID id);

    void markAllRead();

    void deleteNotification(UUID id);
}
