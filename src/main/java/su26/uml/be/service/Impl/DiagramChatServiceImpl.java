package su26.uml.be.service.Impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.config.anythingllm.AnythingLlmClient;
import su26.uml.be.dto.request.DiagramChatRequest;
import su26.uml.be.dto.response.AiResponseKind;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.AnythingLlmChatResponse;
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
import su26.uml.be.service.SystemConfigCacheService;
import su26.uml.be.service.adminDashboard.ActivityTrackerService;
import su26.uml.be.service.DiagramChatService;
import su26.uml.be.service.QuotaService;
import su26.uml.be.service.RateLimiterService;
import su26.uml.be.service.adminDashboard.AiGenerationLogService;
import org.springframework.security.core.context.SecurityContextHolder;

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

    private final AiChatSessionRepository chatSessionRepository;
    private final AiChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AnythingLlmClient anythingLlmClient;
    private final AiGenerationLogService aiGenerationLogService;
    private final SystemConfigCacheService systemConfigCacheService;
    private final ActivityTrackerService activityTracker;
    private final QuotaService quotaService;
    private final RateLimiterService rateLimiterService;

    @Override
    public ApiResponse<DiagramChatResponse> chat(String email, DiagramChatRequest request) {
        User user = getCurrentUser(email);
        UUID uid = user.getId();
        String userId = uid.toString();

        validateChatRequest(request);

        boolean isAdmin = isCurrentUserAdmin();

        // Rate limit — áp cho CẢ admin (429 nếu vượt ngưỡng 10s/60s).
        rateLimiterService.checkOrThrow(uid, isAdmin);

        // Quota — admin BYPASS (không trừ) nhưng request vẫn được ghi log & tính vào tổng lượt.
        // 1 message = trừ đúng 1 quota (dù AI Runtime gọi LLM nhiều lần). 402 nếu hết.
        boolean reserved = false;
        if (!isAdmin) {
            quotaService.reserveAiRequest(uid);
            reserved = true;
        }

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

            // Snapshot provider/model từ workspace trước khi gọi AI
            var activeConfig = systemConfigCacheService.getActiveConfig();
            String provider = activeConfig.provider();
            String modelName = activeConfig.modelName();

            // Gọi AI với cơ chế Error Reflection + token extraction
            AiChatResult result;
            try {
                result = callAiWithRetry(promptBuilder.toString(), session.getAnythingSessionId(), MAX_RETRIES);
            } catch (AppException e) {
                logAiError(session.getAnythingSessionId(), userId, request.getMessage(), e.getMessage(), provider, modelName);
                throw e;
            } catch (RuntimeException e) {
                logAiError(session.getAnythingSessionId(), userId, request.getMessage(), e.getMessage(), provider, modelName);
                throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
            }

            DiagramChatResponse response = result.diagramResponse();

            // Đồng bộ sessionId
            response.setSessionId(session.getAnythingSessionId());

            // Question box KHÔNG tính 1 lượt: nếu AI còn hỏi lại (kind=QUESTIONS) thì hoàn lại
            // lượt vừa reserve. Chỉ khi AI thôi hỏi (DIAGRAM/REPLY) mới giữ lượt → cả vòng
            // hỏi–đáp tính đúng 1 request.
            if (reserved && response.getKind() == AiResponseKind.QUESTIONS) {
                quotaService.rollbackAiRequest(uid);
                reserved = false; // tránh catch bên dưới rollback lần 2
            }

            updateSessionTitleIfNeeded(session, request.getMessage());

            saveUserMessage(userId, session.getId(), request.getMessage());

            // Lưu phản hồi dưới dạng JSON String vào DB
            String rawAnswer = objectMapper.writeValueAsString(response);
            saveAssistantMessage(
                    userId,
                    session.getId(),
                    rawAnswer,
                    modelName,
                    response.getSources()
            );

            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);

            // Async: log AI generation (không block response)
            logAiGeneration(result, userId, session.getAnythingSessionId(), request.getMessage(), rawAnswer, provider, modelName);

            activityTracker.trackActivity(email);

            return ApiResponse.success("Chat successfully", response);

        } catch (AppException exception) {
            // Cách A: AI/hệ thống lỗi → hoàn lại 1 lượt (user không mất oan).
            if (reserved) quotaService.rollbackAiRequest(uid);
            throw exception;
        } catch (Exception exception) {
            if (reserved) quotaService.rollbackAiRequest(uid);
            log.error("AI Chat processing error", exception);
            throw new AppException(ErrorCode.CHAT_SESSION_PROCESSING_ERROR);
        }
    }

    /** Admin lấy từ authorities (ROLE_ADMIN) — tránh lazy-load User.role. */
    private boolean isCurrentUserAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private AiChatResult callAiWithRetry(String userPrompt, String anythingSessionId, int maxRetries) {
        String currentPrompt = userPrompt;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                long startTime = System.currentTimeMillis();
                AnythingLlmChatResponse rawResponse = anythingLlmClient.chat(currentPrompt, anythingSessionId);
                long latencyMs = System.currentTimeMillis() - startTime;

                if (rawResponse == null || rawResponse.getError() != null) {
                    throw new RuntimeException("AnythingLLM error: " + (rawResponse != null ? rawResponse.getError() : "null response"));
                }

                String rawJson = rawResponse.getTextResponse();
                DiagramChatResponse parsed = parseAndValidateAiResponse(rawJson);
                if (parsed == null) {
                    throw new RuntimeException("AI returned invalid/empty response");
                }

                // Map sources from RAG
                List<AiSourceDocument> sources = new ArrayList<>();
                if (rawResponse.getSources() != null) {
                    for (var src : rawResponse.getSources()) {
                        sources.add(AiSourceDocument.builder()
                                .title((String) src.get("title"))
                                .snippet((String) src.get("text"))
                                .url((String) src.get("url"))
                                .build());
                    }
                }
                parsed.setSources(sources);

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
        throw new RuntimeException(lastException != null ? lastException.getMessage() : "AI chat failed");
    }

    private record AiChatResult(DiagramChatResponse diagramResponse, AnythingLlmChatResponse response, long latencyMs) {}

    private void logAiError(String sessionId, String userId, String userMessage,
                            String errorMessage, String provider, String modelName) {
        int inputTokens = estimateTokens(userMessage);
        aiGenerationLogService.log(sessionId, userId, inputTokens, 0,
                EstimationMethod.JTOKKIT, 0, false, errorMessage,
                provider, modelName);
    }

    private void logAiGeneration(AiChatResult result, String userId, String sessionId,
                                 String userMessage, String assistantMessage,
                                 String provider, String modelName) {
        int inputTokens = estimateTokens(userMessage);
        int outputTokens = estimateTokens(assistantMessage);
        EstimationMethod method = EstimationMethod.JTOKKIT;

        aiGenerationLogService.log(
                sessionId, userId,
                inputTokens, outputTokens, method,
                result.latencyMs(), true, null,
                provider, modelName
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
        //    Mở rộng từ 4 ký tự gốc sang đầy đủ các loại quote unicode phổ biến để giảm parse-fail do quote lạ.
        cleanJson = cleanJson
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                // Guillemets (Pháp/Nga/Đức…)
                .replace("«", "\"")
                .replace("»", "\"")
                .replace("‹", "'")
                .replace("›", "'")
                // Low/high double quotes (German style)
                .replace("„", "\"")
                .replace("‚", "'");

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

        // Thử parse JSON, nếu không phải JSON (text thường / lời từ chối / chit-chat), fallback về REPLY
        try {
            if (cleanJson.startsWith("[")) {
                // E-03: nếu AI trả [a, b, c], ưu tiên lấy phần tử DIAGRAM (state cuối cùng);
                // nếu không có diagram thì lấy phần tử đầu. Tuyệt đối KHÔNG lấy đại phần tử 0 vì có thể
                // là reply/questions phụ đi kèm → mất state thật.
                List<DiagramChatResponse> list = objectMapper.readValue(cleanJson, new TypeReference<List<DiagramChatResponse>>() {});
                if (list != null && !list.isEmpty()) {
                    DiagramChatResponse chosen = list.stream()
                            .filter(r -> r != null && r.getKind() == AiResponseKind.DIAGRAM)
                            .findFirst()
                            .orElse(list.get(0));
                    validateAiResponse(chosen);
                    return chosen;
                }
            }
            DiagramChatResponse res = objectMapper.readValue(cleanJson, DiagramChatResponse.class);
            validateAiResponse(res);
            return res;
        } catch (Exception e) {
            // E-05: trước đây fallback REPLY giữ nguyên rawResponse (kể cả khi raw là JSON lỗi) →
            // user thấy JSON rỗng rỗng trong khung chat. Giờ trả message thân thiện để user hiểu
            // và mô tả lại.
            log.warn("AI returned non-JSON response or invalid JSON. Falling back to REPLY. Raw: {}", rawResponse);
            String friendly = "AI đã trả về kết quả không đúng định dạng JSON. "
                    + "Bro thử mô tả lại yêu cầu bằng câu ngắn gọn hơn giúp tôi nhé.";
            return DiagramChatResponse.builder()
                    .kind(AiResponseKind.REPLY)
                    .answer(friendly)
                    .build();
        }
    }

    private void validateAiResponse(DiagramChatResponse response) {
        if (response == null) {
            throw new RuntimeException("AI returned null response object");
        }
        if (response.getKind() == null) {
            throw new RuntimeException("AI response missing 'kind' field (must be 'reply', 'diagram', or 'questions')");
        }
        if (response.getKind() == AiResponseKind.DIAGRAM) {
            if (response.getDiagramType() != null && !response.getDiagramType().isBlank()) {
                String dt = response.getDiagramType().toLowerCase();
                java.util.Set<String> validDiagramTypes = java.util.Set.of("class", "usecase", "activity", "component", "state");
                if (!validDiagramTypes.contains(dt)) {
                    throw new RuntimeException("Invalid diagramType: '" + response.getDiagramType() + "'. Allowed: " + validDiagramTypes);
                }
            }
            if (response.getNodes() != null) {
                java.util.Set<String> validNodeTypes = java.util.Set.of("action", "decision", "start", "final", "fork", "cls", "class", "component", "usecase", "actor", "note", "package");
                for (var node : response.getNodes()) {
                    if (node.getType() == null || !validNodeTypes.contains(node.getType().toLowerCase())) {
                        throw new RuntimeException("Invalid node type: '" + (node != null ? node.getType() : "null") + "'. Allowed: " + validNodeTypes);
                    }
                    // Auto-alias: AI đôi khi trả "class" thay vì "cls" (vì prompt
                    // cũng dùng chữ "class" trong diagramType). Map về "cls" để
                    // FE render đúng component.
                    if ("class".equalsIgnoreCase(node.getType())) {
                        node.setType("cls");
                    }
                }

                // E-01 (nâng cấp): parentId hợp lệ
                // - Nếu node khai báo parentId != null, BẮT BUỘC phải có 1 node khác
                //   trong cùng response có id == parentId. Nếu không → throw để retry.
                // - Actor tuyệt đối KHÔNG được có parentId (system prompt yêu cầu actor ngoài package).
                java.util.Set<String> nodeIds = response.getNodes().stream()
                        .map(su26.uml.be.dto.response.AiNodeDto::getId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());
                for (var node : response.getNodes()) {
                    if (node == null) continue;
                    String parentId = node.getParentId();
                    if (parentId != null && !parentId.isBlank()) {
                        if (!nodeIds.contains(parentId)) {
                            throw new RuntimeException("Node '" + node.getId() + "' has parentId='" + parentId
                                    + "' but no node with that id exists in the response. "
                                    + "Make sure the parent node is included in the nodes array.");
                        }
                    }
                    if ("actor".equalsIgnoreCase(node.getType()) && parentId != null && !parentId.isBlank()) {
                        throw new RuntimeException("Node '" + node.getId() + "' is type 'actor' but has parentId='"
                                + parentId + "'. Actors must stay outside any package (parentId must be null).");
                    }
                }
            }
            if (response.getEdges() != null) {
                java.util.Set<String> validRelations = java.util.Set.of("inheritance", "realization", "association", "aggregation", "composition", "dependency", "include", "extend", "control-flow", "transition", "note-link", "self-transition");
                for (var edge : response.getEdges()) {
                    if (edge.getRelation() == null || !validRelations.contains(edge.getRelation().toLowerCase())) {
                        throw new RuntimeException("Invalid edge relation: '" + (edge != null ? edge.getRelation() : "null") + "'. Allowed: " + validRelations);
                    }
                }
            }
        }

        // Validate QUESTIONS mode: option bắt buộc cho single/multiple.
        // Thay đổi rule mới: ép AI luôn đưa options khi mode = single/multiple,
        // chỉ mode = "text" mới được phép options null/empty.
        if (response.getKind() == AiResponseKind.QUESTIONS && response.getQuestions() != null) {
            java.util.Set<String> validQuestionModes = java.util.Set.of("single", "multiple", "text");
            for (var q : response.getQuestions()) {
                if (q == null) continue;
                String qMode = q.getMode() == null ? "" : q.getMode().toLowerCase();
                if (!validQuestionModes.contains(qMode)) {
                    throw new RuntimeException("Invalid question mode: '" + q.getMode() + "'. Allowed: " + validQuestionModes);
                }
                // single/multiple: bắt buộc có options với >= 2 item
                if ("single".equals(qMode) || "multiple".equals(qMode)) {
                    if (q.getOptions() == null || q.getOptions().size() < 2) {
                        throw new RuntimeException("Question '" + q.getId() + "' has mode='" + q.getMode()
                                + "' but options is null or has < 2 items. "
                                + "For single/multiple mode, you MUST provide at least 2 options. "
                                + "If a free-form answer is needed, use mode='text' instead.");
                    }
                }
            }
        }
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