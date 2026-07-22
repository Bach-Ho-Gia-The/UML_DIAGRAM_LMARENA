package su26.uml.be.features.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.admin.dto.NotificationResponse;
import su26.uml.be.features.admin.service.NotificationService;

import java.util.UUID;

@RestController
@RequestMapping("/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Notifications", description = "Admin dashboard system notifications (read/dismiss). Creation is internal only.")
public class NotificationController {

    NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List notifications",
            description = "Paginated list of system notifications, sorted by createdAt desc. " +
                    "Optional filter: ?read=false for unread only.")
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Boolean read) {
        Page<NotificationResponse> result = notificationService.getNotifications(pageable, read);
        return ApiResponse.success("OK", result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification detail")
    public ApiResponse<NotificationResponse> getNotification(@PathVariable UUID id) {
        NotificationResponse result = notificationService.getNotification(id);
        return ApiResponse.success("OK", result);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ApiResponse.<Void>builder().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ApiResponse<Void> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ApiResponse.<Void>builder().build();
    }
}