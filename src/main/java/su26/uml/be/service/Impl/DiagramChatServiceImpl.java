package su26.uml.be.service.Impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.config.anythingllm.AnythingLlmClient;
import su26.uml.be.config.anythingllm.AnythingLlmProperties;
import su26.uml.be.dto.request.DiagramChatRequest;
import su26.uml.be.dto.response.*;
import su26.uml.be.entity.*;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.AiChatMessageRepository;
import su26.uml.be.repository.AiChatSessionRepository;
import su26.uml.be.repository.SheetRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.DiagramChatService;

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
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MILLIS = 800L;

    private final AnythingLlmClient anythingLlmClient;
    private final AnythingLlmProperties anythingLlmProperties;
    private final AiChatSessionRepository chatSessionRepository;
    private final AiChatMessageRepository chatMessageRepository;
    private final SheetRepository sheetRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    @Override
    public ApiResponse<DiagramChatResponse> chat(String email, DiagramChatRequest request) {
        User user = getCurrentUser(email);
        String userId = user.getId().toString();

        validateChatRequest(request);

        try {
            AiChatSessionDocument session = resolveSession(userId, request.getSessionId());

            // --- Lấy dữ liệu bản vẽ hiện tại để làm ngữ cảnh (Context) cho AI ---
            String currentDiagramContext = "";
            if (request.getSheetId() != null && !request.getSheetId().isBlank()) {
                try {
                    UUID sheetUuid = UUID.fromString(request.getSheetId());
                    Sheet sheet = sheetRepository.findById(sheetUuid).orElse(null);
                    if (sheet != null && sheet.getDiagramData() != null) {
                        currentDiagramContext = "\n\nCURRENT DIAGRAM STATE (JSON): " + sheet.getDiagramData();
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch sheet context for AI: {}", e.getMessage());
                }
            }

            AnythingLlmChatResponse anythingResponse = callAnythingLlmWithRetry(
                    request.getMessage() + currentDiagramContext,
                    session.getAnythingSessionId()
            );

            String rawAnswer = cleanAnswer(anythingResponse.getTextResponse());

            if (rawAnswer.isBlank()) {
                throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
            }

            AiResponseContent parsedContent = parseAiResponse(rawAnswer);

            // --- Logic State Merger ---
            String updatedDiagramData = null;
            if (request.getSheetId() != null && !request.getSheetId().isBlank()) {
                UUID sheetUuid;
                try {
                    sheetUuid = UUID.fromString(request.getSheetId());
                } catch (IllegalArgumentException e) {
                    throw new AppException(ErrorCode.INVALID_REQUEST);
                }

                Sheet sheet = sheetRepository.findById(sheetUuid)
                        .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));

                updatedDiagramData = mergeState(sheet.getDiagramData(), parsedContent.getActions());
                sheet.setDiagramData(updatedDiagramData);
                sheetRepository.save(sheet);
            }

            updateSessionTitleIfNeeded(session, request.getMessage());

            List<AiSourceDocument> sourceDocuments = mapSources(anythingResponse.getSources());

            saveUserMessage(userId, session.getId(), request.getMessage());

            saveAssistantMessage(
                    userId,
                    session.getId(),
                    parsedContent.getMessage(),
                    anythingLlmProperties.modelName(),
                    sourceDocuments,
                    parsedContent.getQuestions().stream()
                            .map(q -> AiChatMessageDocument.QuestionData.builder()
                                    .title(q.getTitle())
                                    .type(q.getType())
                                    .options(q.getOptions())
                                    .build())
                            .toList()
            );

            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);

            DiagramChatResponse response = DiagramChatResponse.builder()
                    .answer(parsedContent.getMessage())
                    .questions(parsedContent.getQuestions().stream()
                            .map(q -> DiagramChatResponse.QuestionResponse.builder()
                                    .title(q.getTitle())
                                    .type(q.getType())
                                    .options(q.getOptions())
                                    .build())
                            .toList())
                    .actions(parsedContent.getActions())
                    .newState(updatedDiagramData)
                    .sessionId(session.getAnythingSessionId())
                    .sources(anythingResponse.getSources() == null ? List.of() : anythingResponse.getSources())
                    .build();

            return ApiResponse.success("Chat successfully", response);

        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.CHAT_SESSION_PROCESSING_ERROR);
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
                        .map(message -> DiagramChatHistoryResponse.MessageItem.builder()
                                .role(message.getRole())
                                .content(message.getContent())
                                .modelName(message.getModelName())
                                .questions(message.getQuestions() == null ? List.of() : message.getQuestions().stream()
                                                                                        .map(q -> DiagramChatResponse.QuestionResponse.builder()
                                                                                                  .title(q.getTitle())
                                                                                                  .type(q.getType())
                                                                                                  .options(q.getOptions())
                                                                                                  .build())
                                                                                        .toList())
                                .createdAt(message.getCreatedAt())
                                .build()
                        )
                        .toList())
                .build();

        return ApiResponse.success("Get chat history successfully", response);
    }

    @SuppressWarnings("unchecked")
    private String mergeState(String currentData, List<CanvasActionResponse> actions) {
        if (actions == null || actions.isEmpty()) {
            return currentData;
        }

        try {
            Map<String, Object> state;
            if (currentData == null || currentData.isBlank() || currentData.equals("{}")) {
                state = new java.util.HashMap<>();
                state.put("nodes", new ArrayList<>());
                state.put("edges", new ArrayList<>());
                state.put("diagramType", "activity"); // Mặc định ban đầu
            } else {
                state = objectMapper.readValue(currentData, Map.class);
            }

            // Đảm bảo nodes và edges luôn là ArrayList để có thể modify
            List<Map<String, Object>> nodes = new ArrayList<>((List<Map<String, Object>>) state.getOrDefault("nodes", new ArrayList<>()));
            List<Map<String, Object>> edges = new ArrayList<>((List<Map<String, Object>>) state.getOrDefault("edges", new ArrayList<>()));

            for (CanvasActionResponse action : actions) {
                String type = action.getType();
                if (type == null || action.getData() == null || !(action.getData() instanceof Map)) continue;
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) action.getData();

                String id = (String) data.get("id");

                switch (type.toUpperCase()) {
                    case "ADD_NODE" -> {
                        if (id != null) {
                            nodes.removeIf(node -> id.equals(node.get("id")));
                        }
                        nodes.add(new java.util.HashMap<>(data));
                    }
                    case "UPDATE_NODE" -> {
                        if (id != null) {
                            for (int i = 0; i < nodes.size(); i++) {
                                if (id.equals(nodes.get(i).get("id"))) {
                                    Map<String, Object> existingNode = new java.util.HashMap<>(nodes.get(i));
                                    
                                    // Xử lý merge các trường top-level (type, position...)
                                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                                        if (entry.getKey().equals("data") && existingNode.get("data") instanceof Map) {
                                            // Nếu là trường "data" lồng nhau, thực hiện merge sâu
                                            Map<String, Object> existingNestedData = new java.util.HashMap<>((Map<String, Object>) existingNode.get("data"));
                                            Map<String, Object> newNestedData = (Map<String, Object>) entry.getValue();
                                            existingNestedData.putAll(newNestedData);
                                            existingNode.put("data", existingNestedData);
                                        } else if (!entry.getKey().equals("id")) {
                                            existingNode.put(entry.getKey(), entry.getValue());
                                        }
                                    }
                                    nodes.set(i, existingNode);
                                    break;
                                }
                            }
                        }
                    }
                    case "DELETE_NODE" -> {
                        if (id != null) {
                            nodes.removeIf(node -> id.equals(node.get("id")));
                            edges.removeIf(edge -> id.equals(edge.get("source")) || id.equals(edge.get("target")));
                        }
                    }
                    case "ADD_EDGE" -> {
                        if (id != null) {
                            edges.removeIf(edge -> id.equals(edge.get("id")));
                        }
                        edges.add(new java.util.HashMap<>(data));
                    }
                    case "DELETE_EDGE" -> {
                        if (id != null) {
                            edges.removeIf(edge -> id.equals(edge.get("id")));
                        }
                    }
                    default -> log.warn("Unknown action type: {}", type);
                }
            }

            // Tự động nhận diện diagramType dựa trên các node hiện có
            String detectedType = detectDiagramType(nodes);
            if (detectedType != null) {
                state.put("diagramType", detectedType);
            }

            state.put("nodes", nodes);
            state.put("edges", edges);
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            log.error("Failed to merge state. Current data: {}, Actions: {}", currentData, actions, e);
            return currentData;
        }
    }

    private String detectDiagramType(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) return null;
        
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");
            if (type == null && node.get("data") instanceof Map) {
                type = (String) ((Map<String, Object>) node.get("data")).get("type");
            }
            
            if ("lifeline".equalsIgnoreCase(type)) return "sequence";
            if ("cls".equalsIgnoreCase(type)) return "class";
            if ("usecase".equalsIgnoreCase(type)) return "usecase";
            if ("component".equalsIgnoreCase(type)) return "component";
        }
        return "activity"; // Fallback mặc định
    }

    private AiResponseContent parseAiResponse(String rawAnswer) {
        String cleanedJson = rawAnswer.trim();

        // 1. Cố gắng tìm JSON block trong markdown if any
        Pattern jsonPattern = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?\\})\\s*```");
        Matcher matcher = jsonPattern.matcher(cleanedJson);
        if (matcher.find()) {
            cleanedJson = matcher.group(1).trim();
        } else {
            // 2. Nếu không có code block, cố gắng tìm cặp dấu ngoặc {} đầu tiên và cuối cùng
            int firstBrace = cleanedJson.indexOf('{');
            int lastBrace = cleanedJson.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleanedJson = cleanedJson.substring(firstBrace, lastBrace + 1).trim();
            }
        }

        // 3. Tự động sửa các lỗi JSON phổ biến của AI
        cleanedJson = fixAiJson(cleanedJson);

        try {
            AiResponseContent content = objectMapper.readValue(cleanedJson, AiResponseContent.class);

            // Bổ sung logic: Nếu message trong JSON bị trống nhưng bên ngoài JSON có text, lấy text đó làm message
            if ((content.getMessage() == null || content.getMessage().isBlank()) && !rawAnswer.equals(cleanedJson)) {
                String textOnly = rawAnswer.replace("```json", "").replace("```", "").replace(cleanedJson, "").trim();
                if (!textOnly.isBlank()) {
                    content.setMessage(textOnly);
                }
            }

            // NÂNG CẤP: Nếu AI trả về format lồng questions bên trong actions (type: ask_questions hoặc type: questions)
            if (content.getActions() != null && (content.getQuestions() == null || content.getQuestions().isEmpty())) {
                for (CanvasActionResponse action : content.getActions()) {
                    boolean isQuestionType = "ask_questions".equalsIgnoreCase(action.getType()) || "questions".equalsIgnoreCase(action.getType());
                    if (isQuestionType && action.getData() != null) {
                        Object questionsObj = null;
                        if (action.getData() instanceof Map) {
                            questionsObj = ((Map<?, ?>) action.getData()).get("questions");
                            if (questionsObj == null) {
                                // Nếu data là Map nhưng không có key "questions", có thể data chính là list các câu hỏi (dù hiếm)
                                log.warn("Action data is Map but no 'questions' key found.");
                            }
                        } else if (action.getData() instanceof List) {
                            // TRƯỜNG HỢP MỚI: data chính là List các câu hỏi
                            questionsObj = action.getData();
                        }

                        if (questionsObj instanceof List) {
                            try {
                                String questionsJson = objectMapper.writeValueAsString(questionsObj);
                                List<AiResponseContent.QuestionContent> extractedQuestions = objectMapper.readValue(
                                    questionsJson, 
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, AiResponseContent.QuestionContent.class)
                                );
                                if (content.getQuestions() == null) {
                                    content.setQuestions(new ArrayList<>());
                                }
                                content.getQuestions().addAll(extractedQuestions);
                            } catch (Exception e) {
                                log.warn("Failed to extract nested questions from action: {}", e.getMessage());
                            }
                        }
                    }
                }
            }

            // Xử lý từng câu hỏi trong danh sách
            if (content.getQuestions() != null && !content.getQuestions().isEmpty()) {
                for (AiResponseContent.QuestionContent q : content.getQuestions()) {
                    // NÂNG CẤP: Nếu AI trả về câu hỏi nhưng không có options, tự động gợi ý luôn
                    if (q.getOptions() == null || q.getOptions().isEmpty()) {
                        List<String> suggested = suggestOptions(q.getTitle());
                        if (!suggested.isEmpty()) {
                            q.setOptions(suggested);
                            q.setType("single_select");
                        } else {
                            q.setOptions(Collections.emptyList());
                            q.setType("text");
                        }
                    } else {
                        // Giữ nguyên options từ AI
                        q.setOptions(q.getOptions());
                    }
                }
            } else {
                // TỰ ĐỘNG BÓC TÁCH: Nếu mảng questions trống, quét trong message tìm danh sách đánh số
                List<AiResponseContent.QuestionContent> extracted = extractQuestionsFromText(content.getMessage());
                content.setQuestions(extracted);
            }

            return content;
        } catch (JsonProcessingException e) {
            // Fallback: Trả về text thuần và cố gắng bóc tách câu hỏi từ đó
            List<AiResponseContent.QuestionContent> extracted = extractQuestionsFromText(rawAnswer);
            return AiResponseContent.builder()
                    .message(rawAnswer)
                    .questions(extracted)
                    .build();
        }
    }

    /**
     * Quét văn bản để tìm các câu hỏi dạng danh sách đánh số (1. , 2., ...) 
     * để tự động tạo Question Box.
     */
    private List<AiResponseContent.QuestionContent> extractQuestionsFromText(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        List<AiResponseContent.QuestionContent> extracted = new ArrayList<>();
        // Regex tìm các dòng bắt đầu bằng số và dấu chấm (ví dụ: "1. Bạn muốn...")
        Pattern questionPattern = Pattern.compile("(?m)^\\s*\\d+[.)]\\s*(.*\\?)");
        Matcher matcher = questionPattern.matcher(text);

        while (matcher.find()) {
            String qText = matcher.group(1).trim();
            if (!qText.isBlank()) {
                // Tự động gợi ý options dựa trên nội dung câu hỏi để bro được "chọn" thay vì nhập tay
                List<String> options = suggestOptions(qText);
                
                extracted.add(AiResponseContent.QuestionContent.builder()
                        .title(qText)
                        .type(options.isEmpty() ? "text" : "single_select")
                        .options(options)
                        .build());
            }
        }
        
        return extracted;
    }

    /**
     * Gợi ý các lựa chọn (options) thông minh dựa trên nội dung câu hỏi 
     * để người dùng có thể chọn nhanh thay vì nhập tay.
     */
    private List<String> suggestOptions(String question) {
        String q = question.toLowerCase();
        List<String> options = new ArrayList<>();

        if (q.contains("actor") || q.contains("tác nhân")) {
            options.addAll(List.of("Khách hàng", "Nhân viên", "Quản lý", "Hệ thống bên ngoài"));
        } else if (q.contains("chức năng") || q.contains("use case")) {
            options.addAll(List.of("Đăng nhập/Đăng ký", "Quản lý hồ sơ", "Tìm kiếm", "Báo cáo thống kê"));
        } else if (q.contains("thanh toán")) {
            options.addAll(List.of("Thanh toán tiền mặt", "Chuyển khoản ngân hàng", "Ví điện tử (Momo/ZaloPay)", "Thẻ tín dụng"));
        } else if (q.contains("phạm vi") || q.contains("boundary")) {
            options.addAll(List.of("Chỉ hệ thống nội bộ", "Hệ thống công khai (Web/App)", "Bao gồm cả quản lý kho", "Tất cả các bên liên quan"));
        } else if (q.contains("có") && q.contains("không")) {
            options.addAll(List.of("Có, chắc chắn rồi", "Không, bỏ qua đi", "Để tôi suy nghĩ thêm"));
        }

        return options;
    }

    /**
     * Tự động sửa lỗi JSON phổ biến: thiếu dấu phẩy, dấu ngoặc kép thông minh...
     */
    private String fixAiJson(String json) {
        if (json == null || json.isEmpty()) return json;

        return json
            // Thay thế dấu ngoặc kép thông minh “ ” và ‘ ’ bằng "
            .replace("“", "\"").replace("”", "\"")
            .replace("‘", "\"").replace("’", "\"")
            // Sửa lỗi thiếu dấu phẩy giữa các trường (ví dụ: "field1": "val" "field2": "val")
            .replaceAll("(\\\"\\s*:\\s*\\\"[^\\\"]*\\\")\\s*(\\\"\\s*[a-zA-Z0-9_]+\\\"\\s*:)", "$1, $2")
            // Sửa lỗi thiếu dấu phẩy sau mảng (ví dụ: "options": [...] "next_field": ...)
            .replaceAll("(])\\s*(\\\"\\s*[a-zA-Z0-9_]+\\\"\\s*:)", "$1, $2")
            .trim();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class AiResponseContent {
        private String message;
        private List<QuestionContent> questions;
        private List<CanvasActionResponse> actions;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class QuestionContent {
            @JsonAlias("question")
            @JsonProperty("title")
            private String title;
            private String type;
            private List<String> options;
            
            // Setter bổ sung để hỗ trợ cả field "question" nếu AI trả về
            @JsonProperty("question")
            public void setQuestion(String question) {
                this.title = question;
            }
        }
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

    private AnythingLlmChatResponse callAnythingLlmWithRetry(String prompt, String sessionId) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                AnythingLlmChatResponse response =
                        anythingLlmClient.chat(prompt, sessionId);

                if (response == null) {
                    throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
                }

                if (response.getError() != null && !response.getError().isBlank()) {
                    throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
                }

                return response;

            } catch (AppException exception) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw exception;
                }

                sleepBeforeRetry();

            } catch (Exception exception) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
                }

                sleepBeforeRetry();
            }
        }

        throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.ANYTHING_LLM_ERROR);
        }
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
            List<AiSourceDocument> sources,
            List<AiChatMessageDocument.QuestionData> questions
    ) {
        chatMessageRepository.save(
                AiChatMessageDocument.builder()
                        .chatSessionId(chatSessionId)
                        .userId(userId)
                        .role(ROLE_ASSISTANT)
                        .content(content)
                        .mode(CHAT_MODE)
                        .modelName(modelName)
                        .questions(questions)
                        .sources(sources)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }

        return answer
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("\\[END CONTEXT \\d+\\]", "") // Xóa markers của AnythingLLM
                .replaceAll("\\[\\d+\\]", "")             // Xóa các trích dẫn [1], [2]...
                .trim();
    }

    private List<AiSourceDocument> mapSources(List<Map<String, Object>> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }

        return sources.stream()
                .map(this::mapSource)
                .toList();
    }

    private AiSourceDocument mapSource(Map<String, Object> source) {
        return AiSourceDocument.builder()
                .title(toStringValue(source.get("title")))
                .url(toStringValue(source.get("url")))
                .score(toBigDecimal(source.get("score")))
                .snippet(shorten(toStringValue(source.get("text")), SOURCE_SNIPPET_MAX_LENGTH))
                .build();
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

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value.toString());
        } catch (Exception exception) {
            return null;
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