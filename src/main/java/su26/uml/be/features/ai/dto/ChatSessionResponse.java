package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSessionResponse {

    String sessionId;

    String title;

    String status;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}