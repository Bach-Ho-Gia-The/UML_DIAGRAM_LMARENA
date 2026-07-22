package su26.uml.be.features.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import su26.uml.be.features.admin.dto.NotificationResponse;
import su26.uml.be.common.constant.enums.NotificationSeverity;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(String type, String title, String message, NotificationSeverity severity);

    Page<NotificationResponse> getNotifications(Pageable pageable, Boolean read);

    NotificationResponse getNotification(UUID id);

    void markRead(UUID id);

    void markAllRead();

    void deleteNotification(UUID id);
}