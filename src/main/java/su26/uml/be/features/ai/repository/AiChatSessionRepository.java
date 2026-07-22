package su26.uml.be.features.ai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import su26.uml.be.features.ai.entity.AiChatSessionDocument;

import java.util.List;
import java.util.Optional;

public interface AiChatSessionRepository
        extends MongoRepository<AiChatSessionDocument, String> {

    Optional<AiChatSessionDocument>
    findFirstByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);

    Optional<AiChatSessionDocument>
    findByAnythingSessionIdAndUserId(String anythingSessionId, String userId);

    List<AiChatSessionDocument>
    findByUserIdOrderByUpdatedAtDesc(String userId);

    Page<AiChatSessionDocument>
    findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);
}