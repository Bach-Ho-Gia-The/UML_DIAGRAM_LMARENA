package su26.uml.be.features.ai.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ai_chat_sessions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatSessionDocument {

    @Id
    String id;

    @Indexed
    String userId;

    @Indexed(unique = true)
    String anythingSessionId;

    String title;

    String status;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}