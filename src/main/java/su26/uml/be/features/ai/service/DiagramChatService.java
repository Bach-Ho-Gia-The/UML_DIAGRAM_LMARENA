package su26.uml.be.features.ai.service;

import org.springframework.data.domain.Pageable;
import su26.uml.be.features.ai.dto.DiagramChatRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.ai.dto.ChatSessionResponse;
import su26.uml.be.features.ai.dto.DiagramChatHistoryResponse;
import su26.uml.be.features.ai.dto.DiagramChatResponse;
import su26.uml.be.common.response.PagedResponse;

public interface DiagramChatService {

    ApiResponse<DiagramChatResponse> chat(String email, DiagramChatRequest request);

    ApiResponse<ChatSessionResponse> createSession(String email);

    ApiResponse<PagedResponse<ChatSessionResponse>> getSessions(String email, Pageable pageable);

    ApiResponse<PagedResponse<DiagramChatHistoryResponse.MessageItem>> getHistory(
            String email, String sessionId, Pageable pageable);
}