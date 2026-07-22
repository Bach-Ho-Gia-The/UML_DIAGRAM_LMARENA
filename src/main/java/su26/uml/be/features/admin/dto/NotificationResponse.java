package su26.uml.be.features.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "NotificationResponse", description = "Admin dashboard notification.")
public class NotificationResponse {

    @Schema(description = "Notification ID.")
    UUID id;

    @Schema(description = "Notification type key (e.g. anomaly_detected, unknown_model).", example = "anomaly_detected")
    String type;

    @Schema(description = "Short title.", example = "Anomaly Detected")
    String title;

    @Schema(description = "Detailed message.", example = "Suspicious activity detected from IP 192.168.1.1")
    String message;

    @Schema(description = "Severity level.", example = "WARNING")
    String severity;

    @Schema(description = "Whether the notification has been read.", example = "false")
    boolean isRead;

    @Schema(description = "Creation timestamp.")
    LocalDateTime createdAt;
}