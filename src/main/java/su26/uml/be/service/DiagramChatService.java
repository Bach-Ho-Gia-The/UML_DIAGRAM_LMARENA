package su26.uml.be.service;

import org.springframework.data.domain.Pageable;
import su26.uml.be.dto.request.DiagramChatRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.ChatSessionResponse;
import su26.uml.be.dto.response.DiagramChatHistoryResponse;
import su26.uml.be.dto.response.DiagramChatResponse;
import su26.uml.be.dto.response.PagedResponse;

public interface DiagramChatService {

    ApiResponse<DiagramChatResponse> chat(String email, DiagramChatRequest request);

    ApiResponse<ChatSessionResponse> createSession(String email);

    ApiResponse<PagedResponse<ChatSessionResponse>> getSessions(String email, Pageable pageable);

    ApiResponse<PagedResponse<DiagramChatHistoryResponse.MessageItem>> getHistory(
            String email, String sessionId, Pageable pageable);
}