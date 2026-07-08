package su26.uml.be.service.Impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.config.anythingllm.AnythingLlmProperties;
import su26.uml.be.dto.request.DiagramChatRequest;
import su26.uml.be.dto.response.AiResponseKind;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.ChatSessionResponse;
import su26.uml.be.dto.response.DiagramChatHistoryResponse;
import su26.uml.be.dto.response.DiagramChatResponse;
import su26.uml.be.entity.AiChatMessageDocument;
import su26.uml.be.entity.AiChatSessionDocument;
import su26.uml.be.entity.AiSourceDocument;
import su26.uml.be.entity.User;
import su26.uml.be.enums.EstimationMethod;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.AiChatMessageRepository;
import su26.uml.be.repository.AiChatSessionRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.DiagramChatService;
import su26.uml.be.service.ai.UmlArchitect;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagramChatServiceImpl implements DiagramChatService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";
    private static final String CHAT_MODE = "chat";
    private static final String SESSION_STATUS_ACTIVE = "ACTIVE";
    private static final String DEFAULT_SESSION_TITLE = "New chat";
    private static final int SOURCE_SNIPPET_MAX_LENGTH = 500;
    private static final int MAX_RETRIES = 3;

    private final AnythingLlmProperties anythingLlmProperties;
    private final AiChatSessionRepository chatSessionRepository;
    private final AiChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final UmlArchitect umlArchitect;
    private final AiBillingEventPublisher aiBillingEventPublisher;

    @Override
    public ApiResponse<DiagramChatResponse> chat(String email, DiagramChatRequest request) {
        User user = getCurrentUser(email);
        String userId = user.getId().toString();

        validateChatRequest(request);

        try {
            AiChatSessionDocument session = resolveSession(userId, request.getSessionId());

            // Xây dựng prompt với context được định dạng dễ đọc cho AI
            StringBuilder promptBuilder = new StringBuilder();
            
            if (request.getCurrentNodes() != null && !request.getCurrentNodes().isEmpty()) {
                promptBuilder.append("--- CANVAS CONTEXT (SHORTHAND) ---\n");
                for (var n : request.getCurrentNodes()) {
                    promptBuilder.append(String.format("%s:%s(%s)", n.getId(), n.getType(), n.getLabel()));
                    if (n.getStereotype() != null) promptBuilder.append("<<").append(n.getStereotype()).append(">>");
                    
                    List<String> details = new ArrayList<>();
                    if (n.getAttributes() != null && !n.getAttributes().isEmpty()) 
                        details.add("a:[" + String.join(",", n.getAttributes()) + "]");
                    if (n.getMethods() != null && !n.getMethods().isEmpty()) 
                        details.add("m:[" + String.join(",", n.getMethods()) + "]");
                    
                    if (!details.isEmpty()) {
                        promptBuilder.append("{").append(String.join(",", details)).append("}");
                    }
                    promptBuilder.append("\n");
                }
                
                if (request.getCurrentEdges() != null && !request.getCurrentEdges().isEmpty()) {
                    promptBuilder.append("Edges: ");
                    for (var e : request.getCurrentEdges()) {
                        promptBuilder.append(String.format("[%s:%s->%s(%s)] ", 
                            e.getId(), e.getSource(), e.getTarget(), e.getRelation()));
                    }
                    promptBuilder.append("\n");
                }
                promptBuilder.append("----------------------------------\n\n");
            }

            promptBuilder.append("YÊU CẦU CỦA NGƯỜI DÙNG: ").append(request.getMessage()).append("\n\n");
            promptBuilder.append("LƯU Ý QUAN TRỌNG: Bạn phải trả về TOÀN BỘ sơ đồ cuối cùng (bao gồm cả các node cũ muốn giữ lại và các node mới). ");
            promptBuilder.append("Nếu một node có trong danh sách trên nhưng không có trong kết quả JSON của bạn, nó sẽ bị xóa khỏi màn hình.");

            // Gọi AI với cơ chế Error Reflection + token extraction
            AiChatResult result = callAiWithRetry(promptBuilder.toString(), MAX_RETRIES);

            if (result == null) {
                throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
            }

            DiagramChatResponse response = result.diagramResponse();

            // Đồng bộ sessionId
            response.setSessionId(session.getAnythingSessionId());

            updateSessionTitleIfNeeded(session, request.getMessage());

            saveUserMessage(userId, session.getId(), request.getMessage());

            // Lưu phản hồi dưới dạng JSON String vào DB
            String rawAnswer = objectMapper.writeValueAsString(response);
            saveAssistantMessage(
                    userId,
                    session.getId(),
                    rawAnswer,
                    anythingLlmProperties.modelName(),
                    response.getSources()
            );

            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);

            // Async billing: fire and forget (không block response)
            fireBillingAsync(result, userId, session.getAnythingSessionId(), request.getMessage(), rawAnswer);

            return ApiResponse.success("Chat successfully", response);

        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("AI Chat processing error", exception);
            throw new AppException(ErrorCode.CHAT_SESSION_PROCESSING_ERROR);
        }
    }

    private AiChatResult callAiWithRetry(String userPrompt, int maxRetries) {
        String currentPrompt = userPrompt;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                long startTime = System.currentTimeMillis();
                ChatResponse rawResponse = umlArchitect.chat(currentPrompt);
                long latencyMs = System.currentTimeMillis() - startTime;

                String rawJson = rawResponse.aiMessage().text();
                DiagramChatResponse parsed = parseAndValidateAiResponse(rawJson);
                if (parsed == null) {
                    throw new RuntimeException("AI returned invalid/empty response");
                }

                return new AiChatResult(parsed, rawResponse, latencyMs);

            } catch (Exception e) {
                log.warn("AI Chat retry {}/{} due to error: {}", i + 1, maxRetries, e.getMessage());
                lastException = e;
                // Error Reflection: Gửi lại lỗi để AI tự sửa
                currentPrompt = userPrompt + "\n\n" +
                        "CẢNH BÁO: Lần trước bạn đã trả về kết quả gây lỗi hệ thống.\n" +
                        "Lỗi: " + e.getMessage() + "\n" +
                        "Hãy sửa lại và chỉ trả về đúng JSON hợp lệ theo định dạng đã quy định.";
            }
        }
        log.error("AI Chat failed after {} retries", maxRetries, lastException);
        return null;
    }

    private record AiChatResult(DiagramChatResponse diagramResponse, ChatResponse response, long latencyMs) {}

    private void fireBillingAsync(AiChatResult result, String userId, String sessionId,
                                   String userMessage, String assistantMessage) {
        TokenUsage usage = result.response().tokenUsage();
        int inputTokens;
        int outputTokens;
        EstimationMethod method;

        if (usage != null) {
            inputTokens = usage.inputTokenCount();
            outputTokens = usage.outputTokenCount();
            method = EstimationMethod.PROVIDER;
        } else {
            inputTokens = estimateTokens(userMessage);
            outputTokens = estimateTokens(assistantMessage);
            method = EstimationMethod.JTOKKIT;
        }

        aiBillingEventPublisher.publishBill(
                sessionId, userId,
                inputTokens, outputTokens, method,
                result.latencyMs(), true, null
        );
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
            Encoding enc = registry.getEncodingForModel(ModelType.GPT_4O_MINI);
            return enc.countTokens(text);
        } catch (Exception e) {
            return text.length() / 4;
        }
    }

    private DiagramChatResponse parseAndValidateAiResponse(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new RuntimeException("AI returned empty response");
        }

        // 1. Loại bỏ các tag suy nghĩ <think>...</think> (nếu có từ DeepSeek)
        String cleanJson = rawResponse.replaceAll("(?s)<think>.*?</think>", "").trim();

        // 2. Chuẩn hóa dấu ngoặc kép (xử lý trường hợp AI nhả ra dấu ngoặc kép thông minh/curly quotes)
        cleanJson = cleanJson.replace("“", "\"").replace("”", "\"").replace("‘", "'").replace("’", "'");

        // 3. Trích xuất JSON từ Markdown code blocks (nếu có)
        Pattern pattern = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");
        Matcher matcher = pattern.matcher(cleanJson);
        if (matcher.find()) {
            cleanJson = matcher.group(1).trim();
        } else {
            // 4. Nếu không có markdown, cố gắng tìm khối { ... } hoặc [ ... ] lớn nhất để loại bỏ văn bản thừa
            int firstBrace = cleanJson.indexOf('{');
            int firstBracket = cleanJson.indexOf('[');
            int lastBrace = cleanJson.lastIndexOf('}');
            int lastBracket = cleanJson.lastIndexOf(']');

            int start = -1;
            int end = -1;

            if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
                start = firstBrace;
                end = lastBrace;
            } else if (firstBracket != -1) {
                start = firstBracket;
                end = lastBracket;
            }

            if (start != -1 && end != -1 && end > start) {
                cleanJson = cleanJson.substring(start, end + 1).trim();
            }
        }

        // 5. Xử lý trường hợp AI nhả ra mảng [ { ... } ]
        if (cleanJson.startsWith("[")) {
            List<DiagramChatResponse> list = objectMapper.readValue(cleanJson, new TypeReference<List<DiagramChatResponse>>() {});
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }

        // 4. Parse JSON Object thông thường
        return objectMapper.readValue(cleanJson, DiagramChatResponse.class);
    }

    @Override
    public ApiResponse<ChatSessionResponse> createSession(String email) {
        User user = getCurrentUser(email);
        String userId = user.getId().toString();

        AiChatSessionDocument session = createNewSessionDocument(userId);

        return ApiResponse.success(
                "Create chat session successfully",
                mapSessionResponse(session)
        );
    }

    @Override
    public ApiResponse<List<ChatSessionResponse>> getSessions(String email) {
        User user = getCurrentUser(email);
        String userId = user.getId().toString();

        List<ChatSessionResponse> response = chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::mapSessionResponse)
                .toList();

        return ApiResponse.success("Get chat sessions successfully", response);
    }

    @Override
    public ApiResponse<DiagramChatHistoryResponse> getHistory(String email, String sessionId) {
        User user = getCurrentUser(email);
        String userId = user.getId().toString();

        if (sessionId == null || sessionId.isBlank()) {
            throw new AppException(ErrorCode.CHAT_SESSION_ID_REQUIRED);
        }

        AiChatSessionDocument session = chatSessionRepository
                .findByAnythingSessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));

        List<AiChatMessageDocument> messages =
                chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(session.getId());

        DiagramChatHistoryResponse response = DiagramChatHistoryResponse.builder()
                .sessionId(session.getAnythingSessionId())
                .messages(messages.stream()
                        .map(message -> {
                            DiagramChatResponse parsed = parseAiResponse(message.getContent());
                            return DiagramChatHistoryResponse.MessageItem.builder()
                                    .role(message.getRole())
                                    .content(message.getContent())
                                    .kind(parsed.getKind())
                                    .summary(parsed.getSummary())
                                    .nodes(parsed.getNodes())
                                    .edges(parsed.getEdges())
                                    .questions(parsed.getQuestions())
                                    .modelName(message.getModelName())
                                    .createdAt(message.getCreatedAt())
                                    .build();
                        })
                        .toList())
                .build();

        return ApiResponse.success("Get chat history successfully", response);
    }

    private AiChatSessionDocument resolveSession(String userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return createNewSessionDocument(userId);
        }

        return chatSessionRepository
                .findByAnythingSessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    private AiChatSessionDocument createNewSessionDocument(String userId) {
        LocalDateTime now = LocalDateTime.now();

        return chatSessionRepository.save(
                AiChatSessionDocument.builder()
                        .userId(userId)
                        .anythingSessionId("uml-chat-" + UUID.randomUUID())
                        .title(DEFAULT_SESSION_TITLE)
                        .status(SESSION_STATUS_ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );
    }

    private void saveUserMessage(String userId, String chatSessionId, String content) {
        chatMessageRepository.save(
                AiChatMessageDocument.builder()
                        .chatSessionId(chatSessionId)
                        .userId(userId)
                        .role(ROLE_USER)
                        .content(content)
                        .mode(CHAT_MODE)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private void saveAssistantMessage(
            String userId,
            String chatSessionId,
            String content,
            String modelName,
            List<AiSourceDocument> sources
    ) {
        chatMessageRepository.save(
                AiChatMessageDocument.builder()
                        .chatSessionId(chatSessionId)
                        .userId(userId)
                        .role(ROLE_ASSISTANT)
                        .content(content)
                        .mode(CHAT_MODE)
                        .modelName(modelName)
                        .sources(sources)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private ChatSessionResponse mapSessionResponse(AiChatSessionDocument session) {
        return ChatSessionResponse.builder()
                .sessionId(session.getAnythingSessionId())
                .title(session.getTitle())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private User getCurrentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void validateChatRequest(DiagramChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new AppException(ErrorCode.CHAT_MESSAGE_REQUIRED);
        }
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }

    private void updateSessionTitleIfNeeded(AiChatSessionDocument session, String userMessage) {
        if (session.getTitle() != null && !session.getTitle().equals(DEFAULT_SESSION_TITLE)) {
            return;
        }

        if (isGreetingOrTooShort(userMessage)) {
            return;
        }

        String title = generateSimpleSessionTitle(userMessage);

        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());

        chatSessionRepository.save(session);
    }

    private String generateSimpleSessionTitle(String message) {
        String title = message.trim()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        title = removeSimplePrefix(title);

        int maxLength = 45;

        if (title.length() <= maxLength) {
            return title;
        }

        return title.substring(0, maxLength).trim() + "...";
    }

    private DiagramChatResponse parseAiResponse(String content) {
        if (content == null || content.isBlank()) {
            return DiagramChatResponse.builder()
                    .kind(AiResponseKind.REPLY)
                    .answer("")
                    .build();
        }

        try {
            // Bây giờ DB lưu thẳng JSON string của DiagramChatResponse
            return objectMapper.readValue(content, DiagramChatResponse.class);
        } catch (Exception e) {
            // Fallback nếu dữ liệu cũ không phải JSON hoặc parse lỗi
            return DiagramChatResponse.builder()
                    .kind(AiResponseKind.REPLY)
                    .answer(content)
                    .build();
        }
    }

    private String removeSimplePrefix(String title) {
        return title
                .replaceFirst("(?i)^tôi muốn\\s+", "")
                .replaceFirst("(?i)^tôi cần\\s+", "")
                .replaceFirst("(?i)^mình muốn\\s+", "")
                .replaceFirst("(?i)^mình cần\\s+", "")
                .replaceFirst("(?i)^em muốn\\s+", "")
                .replaceFirst("(?i)^em cần\\s+", "")
                .replaceFirst("(?i)^hãy giúp tôi\\s+", "")
                .replaceFirst("(?i)^giúp tôi\\s+", "")
                .trim();
    }

    private boolean isGreetingOrTooShort(String message) {
        if (message == null || message.isBlank()) {
            return true;
        }

        String normalized = message.toLowerCase()
                .replaceAll("[\\s,.!?;:]+", " ")
                .trim();

        return normalized.length() < 6
                || normalized.equals("hi")
                || normalized.equals("hello")
                || normalized.equals("hey")
                || normalized.equals("chào")
                || normalized.equals("xin chào")
                || normalized.equals("chào bạn")
                || normalized.equals("alo");
    }
}