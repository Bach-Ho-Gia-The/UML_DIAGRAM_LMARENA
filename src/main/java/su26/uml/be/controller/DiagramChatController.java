package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.dto.request.DiagramChatRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.ChatSessionResponse;
import su26.uml.be.dto.response.DiagramChatHistoryResponse;
import su26.uml.be.dto.response.DiagramChatResponse;
import su26.uml.be.dto.response.PagedResponse;
import su26.uml.be.service.DiagramChatService;

@RestController
@RequestMapping("/diagram-ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Diagram AI Chat", description = "UML diagram AI chat and session APIs.")
@SecurityRequirement(name = "bearerAuth")
public class DiagramChatController {

    DiagramChatService diagramChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Send message to AI",
            description = "Send a message to Diagram AI. Provide sessionId to continue an existing chat."
    )
    public ApiResponse<DiagramChatResponse> chat(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails,

            @Valid @RequestBody DiagramChatRequest request
    ) {
        return diagramChatService.chat(userDetails.getUsername(), request);
    }

    @PostMapping("/chat/sessions")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Create chat session",
            description = "Create a new AI chat session for the current user."
    )
    public ApiResponse<ChatSessionResponse> createSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return diagramChatService.createSession(userDetails.getUsername());
    }

    @GetMapping("/chat/sessions")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get chat sessions (paginated)",
            description = "Paginated list of the current user's AI chat sessions, sorted by updatedAt desc. " +
                    "Defaults: page=0, size=20."
    )
    public ApiResponse<PagedResponse<ChatSessionResponse>> getSessions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails,

            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return diagramChatService.getSessions(userDetails.getUsername(), pageable);
    }

    @GetMapping("/chat/sessions/{sessionId}/messages")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get chat history (paginated)",
            description = "Paginated messages of a specific AI chat session, ordered by createdAt asc. " +
                    "Defaults: page=0, size=50.")
    public ApiResponse<PagedResponse<DiagramChatHistoryResponse.MessageItem>> getHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails userDetails,

            @Parameter(description = "Chat session ID", example = "uml-chat-3f974a86-f7c9-4d5d-b386-0c6110896cb6")
            @PathVariable String sessionId,

            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return diagramChatService.getHistory(userDetails.getUsername(), sessionId, pageable);
    }
}