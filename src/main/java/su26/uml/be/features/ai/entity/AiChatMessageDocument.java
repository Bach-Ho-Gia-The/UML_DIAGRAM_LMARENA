package su26.uml.be.features.ai.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "ai_chat_messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatMessageDocument {

    @Id
    String id;

    @Indexed
    String chatSessionId;

    @Indexed
    String userId;

    String role;

    String content;

    String mode;

    String modelName;

    List<AiSourceDocument> sources;

    LocalDateTime createdAt;
}