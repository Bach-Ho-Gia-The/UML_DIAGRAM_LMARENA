package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import su26.uml.be.dto.response.NotificationResponse;
import su26.uml.be.entity.Notification;
import su26.uml.be.enums.NotificationSeverity;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.mapper.NotificationMapper;
import su26.uml.be.repository.NotificationRepository;
import su26.uml.be.service.NotificationService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository notificationRepository;
    NotificationMapper notificationMapper;

    @Override
    public NotificationResponse createNotification(String type, String title, String message, NotificationSeverity severity) {
        long windowMinutes = severity == NotificationSeverity.CRITICAL ? 15 : 60;
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        long existingCount = notificationRepository.countByTypeAndSeverityAndCreatedAtAfter(type, severity, since);
        if (existingCount > 0) {
            log.info("Rate-limited notification '{}' (severity={}): skipped, already sent within {}min", type, severity, windowMinutes);
            return null;
        }

        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .severity(severity)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created: type={}, severity={}, id={}", type, severity, notification.getId());
        return notificationMapper.toNotificationResponse(notification);
    }

    @Override
    public Page<NotificationResponse> getNotifications(Pageable pageable, Boolean read) {
        Page<Notification> page;
        if (read != null) {
            page = notificationRepository.findByIsReadOrderByCreatedAtDesc(read, pageable);
        } else {
            page = notificationRepository.findByOrderByCreatedAtDesc(pageable);
        }
        return page.map(notificationMapper::toNotificationResponse);
    }

    @Override
    public NotificationResponse getNotification(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return notificationMapper.toNotificationResponse(notification);
    }

    @Override
    public void markRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead() {
        notificationRepository.findByOrderByCreatedAtDesc(Pageable.unpaged())
                .forEach(n -> {
                    if (!n.isRead()) {
                        n.setRead(true);
                        notificationRepository.save(n);
                    }
                });
    }

    @Override
    public void deleteNotification(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.deleteById(id);
    }
}
