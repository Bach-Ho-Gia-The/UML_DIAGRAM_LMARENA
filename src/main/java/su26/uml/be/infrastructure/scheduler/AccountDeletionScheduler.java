package su26.uml.be.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.audit.AuditSystemActor;
import su26.uml.be.features.audit.entity.AuditLog;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.UserStatus;
import su26.uml.be.features.audit.repository.AuditLogRepository;
import su26.uml.be.features.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AccountDeletionScheduler {

    UserRepository userRepository;
    AuditLogRepository auditLogRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    // Chạy lúc 00:00 mỗi ngày, dọn các tài khoản PENDING_DELETE đã quá hạn 30 ngày.
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeExpiredAccounts() {
        List<User> expiredUsers = userRepository
                .findAllByStatusAndDeletionDateLessThanEqual(UserStatus.PENDING_DELETE, LocalDateTime.now());

        if (expiredUsers.isEmpty()) {
            log.info("[AccountDeletion] No expired accounts to purge.");
            return;
        }

        log.info("[AccountDeletion] Purging {} expired account(s)...", expiredUsers.size());

        // Ghi audit TRƯỚC khi xóa, CÙNG transaction: nếu xóa lỗi → rollback cả audit,
        // không bao giờ để tài khoản biến mất mà không có dấu vết ("ai đã xóa?" luôn có đáp án).
        List<AuditLog> auditRows = expiredUsers.stream()
                .map(this::toAutoDeleteAudit)
                .toList();
        auditLogRepository.saveAll(auditRows);

        userRepository.deleteAll(expiredUsers);

        log.info("[AccountDeletion] Successfully purged {} account(s).", expiredUsers.size());
    }

    /** Một dòng audit cho mỗi tài khoản bị dọn, do actor HỆ THỐNG thực hiện. */
    private AuditLog toAutoDeleteAudit(User user) {
        return AuditLog.builder()
                // actorId/actorEmail null: đây là tác vụ hệ thống, không có principal thật.
                .actorName(AuditSystemActor.NAME)
                .action("ACCOUNT_AUTO_DELETE")
                .targetType("USER")
                .targetId(user.getId().toString())
                .detail(buildDetail(user))
                .build();
    }

    private String buildDetail(User user) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("email", user.getEmail());
        detail.put("deletionDate", String.valueOf(user.getDeletionDate()));
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            log.warn("[AccountDeletion] Không serialize được detail audit cho {}: {}",
                    user.getEmail(), e.getMessage());
            return null;
        }
    }
}