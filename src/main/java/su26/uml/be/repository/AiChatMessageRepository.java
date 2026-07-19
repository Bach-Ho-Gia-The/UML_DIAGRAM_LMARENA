package su26.uml.be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import su26.uml.be.entity.AiChatMessageDocument;

import java.util.List;

public interface AiChatMessageRepository
        extends MongoRepository<AiChatMessageDocument, String> {

    List<AiChatMessageDocument> findByChatSessionIdOrderByCreatedAtAsc(String chatSessionId);

    Page<AiChatMessageDocument> findByChatSessionIdOrderByCreatedAtAsc(String chatSessionId, Pageable pageable);

    List<AiChatMessageDocument> findTop10ByChatSessionIdOrderByCreatedAtDesc(String chatSessionId);
}