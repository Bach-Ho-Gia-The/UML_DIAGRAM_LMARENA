package su26.uml.be.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.dto.request.WorkspaceItemCreateRequest;
import su26.uml.be.dto.request.WorkspaceItemDeleteRequest;
import su26.uml.be.dto.request.WorkspaceItemDuplicateRequest;
import su26.uml.be.dto.request.WorkspaceItemMoveRequest;
import su26.uml.be.dto.request.WorkspaceItemUpdateRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.WorkspaceItemResponse;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.Sheet;
import su26.uml.be.entity.User;
import su26.uml.be.entity.WorkspaceItem;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.enums.WorkspaceItemKind;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.mapper.SheetMapper;
import su26.uml.be.mapper.WorkspaceItemMapper;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.repository.SheetRepository;
import su26.uml.be.repository.WorkspaceItemRepository;
import su26.uml.be.service.PlanLimitService;
import su26.uml.be.service.ProjectAccessService;
import su26.uml.be.service.SocketService;
import su26.uml.be.service.WorkspaceItemService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class WorkspaceItemServiceImpl implements WorkspaceItemService {

    static final int MAX_DEPTH = 50;
    static final int MAX_MARKDOWN_BYTES = 2 * 1024 * 1024;
    static final String DEFAULT_DIAGRAM_TYPE = "activity";

    WorkspaceItemRepository workspaceItemRepository;
    ProjectRepository projectRepository;
    SheetRepository sheetRepository;
    WorkspaceItemMapper workspaceItemMapper;
    SheetMapper sheetMapper;
    ProjectAccessService projectAccessService;
    PlanLimitService planLimitService;
    SocketService socketService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<WorkspaceItemResponse>> getWorkspaceItems(UUID projectId, String email) {
        Project project = projectAccessService.getProjectAndValidateAccess(projectId, email);
        List<WorkspaceItem> items = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project);
        return ApiResponse.success("Lấy danh sách mục workspace thành công",
                workspaceItemMapper.toWorkspaceItemResponseList(items));
    }

    @Override
    public ApiResponse<WorkspaceItemResponse> createItem(UUID projectId, String email,
                                                         WorkspaceItemCreateRequest request) {
        Project project = lockProject(projectId);
        User user = projectAccessService.validateAccess(project, email);

        String name = request.getName().trim();
        if (name.isEmpty()) {
            throw new AppException(ErrorCode.WORKSPACE_NAME_REQUIRED);
        }

        List<WorkspaceItem> all = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project);
        WorkspaceItem parent = resolveParent(all, request.getParentId());
        assertDepthAllowed(depthOf(parent) + 1);
        assertNameAvailable(all, request.getParentId(), name, null);

        if (request.getKind() != WorkspaceItemKind.MARKDOWN && request.getContent() != null) {
            throw new AppException(ErrorCode.WORKSPACE_CONTENT_NOT_ALLOWED);
        }

        WorkspaceItem item = workspaceItemMapper.toWorkspaceItem(request);
        item.setName(name);
        item.setProject(project);
        item.setParent(parent);
        item.setCreatedBy(user);

        switch (request.getKind()) {
            case MARKDOWN -> {
                // Giữ nguyên content rỗng "" (file mới tạo) — không được biến thành null
                String content = request.getContent() == null ? "" : request.getContent();
                assertMarkdownSize(content);
                item.setMarkdownContent(content);
            }
            case DIAGRAM -> item.setSheet(
                    createSheetForDiagram(project, name, request.getDiagramType(), request.getDiagramData()));
            case FOLDER -> { /* folder không có payload thêm */ }
        }

        placeAmongSiblings(all, item, request.getParentId(), request.getOrderIndex());
        socketService.broadcastWorkspaceChanged(project.getId());
        log.info("Workspace item created: {} ({}) in project {}", item.getId(), request.getKind(), project.getId());
        return ApiResponse.success("Tạo mục workspace thành công",
                workspaceItemMapper.toWorkspaceItemResponse(item));
    }

    @Override
    public ApiResponse<WorkspaceItemResponse> updateItem(UUID itemId, String email,
                                                         WorkspaceItemUpdateRequest request) {
        WorkspaceItem item = getItem(itemId);
        Project project = lockProject(item.getProject().getId());
        projectAccessService.validateAccess(project, email);
        assertVersionMatches(item, request.getExpectedVersion());

        List<WorkspaceItem> all = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project);

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new AppException(ErrorCode.WORKSPACE_NAME_REQUIRED);
            }
            assertNameAvailable(all, idOf(item.getParent()), name, item.getId());
            request.setName(name);
        }
        if (request.getContent() != null) {
            if (item.getKind() != WorkspaceItemKind.MARKDOWN) {
                throw new AppException(ErrorCode.WORKSPACE_CONTENT_NOT_ALLOWED);
            }
            assertMarkdownSize(request.getContent());
        }

        workspaceItemMapper.updateWorkspaceItem(request, item);

        // Tên sheet là canonical — rename diagram phải sync sheet để API sheet cũ thấy cùng một tên
        if (request.getName() != null && item.getKind() == WorkspaceItemKind.DIAGRAM && item.getSheet() != null) {
            item.getSheet().setName(item.getName());
            sheetRepository.save(item.getSheet());
        }

        WorkspaceItem saved = workspaceItemRepository.save(item);
        socketService.broadcastWorkspaceChanged(project.getId());
        return ApiResponse.success("Cập nhật mục workspace thành công",
                workspaceItemMapper.toWorkspaceItemResponse(saved));
    }

    @Override
    public ApiResponse<WorkspaceItemResponse> moveItem(UUID itemId, String email, WorkspaceItemMoveRequest request) {
        WorkspaceItem item = getItem(itemId);
        Project project = lockProject(item.getProject().getId());
        projectAccessService.validateAccess(project, email);
        assertVersionMatches(item, request.getExpectedVersion());

        List<WorkspaceItem> all = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project);
        UUID targetParentId = request.getParentId();
        WorkspaceItem targetParent = resolveParent(all, targetParentId);

        // Chống cycle: đích không được là chính nó hoặc hậu duệ của nó
        if (targetParentId != null) {
            if (targetParentId.equals(item.getId())) {
                throw new AppException(ErrorCode.WORKSPACE_TREE_CYCLE);
            }
            for (WorkspaceItem cursor = targetParent; cursor != null; cursor = cursor.getParent()) {
                if (cursor.getId().equals(item.getId())) {
                    throw new AppException(ErrorCode.WORKSPACE_TREE_CYCLE);
                }
            }
        }

        assertDepthAllowed(depthOf(targetParent) + 1 + subtreeHeight(all, item));
        assertNameAvailable(all, targetParentId, item.getName(), item.getId());

        UUID oldParentId = idOf(item.getParent());
        item.setParent(targetParent);

        // Dồn lại orderIndex ở folder cũ (nếu thực sự đổi folder), rồi chèn vào folder mới
        if (!Objects.equals(oldParentId, targetParentId)) {
            reindexChildren(all, oldParentId, item);
        }
        placeAmongSiblings(all, item, targetParentId, request.getOrderIndex());

        socketService.broadcastWorkspaceChanged(project.getId());
        return ApiResponse.success("Di chuyển mục workspace thành công",
                workspaceItemMapper.toWorkspaceItemResponse(item));
    }

    @Override
    public ApiResponse<WorkspaceItemResponse> duplicateItem(UUID itemId, String email,
                                                            WorkspaceItemDuplicateRequest request) {
        WorkspaceItem source = getItem(itemId);
        Project project = lockProject(source.getProject().getId());
        User user = projectAccessService.validateAccess(project, email);

        List<WorkspaceItem> all = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project);
        UUID targetParentId = request.getParentId();
        WorkspaceItem targetParent = resolveParent(all, targetParentId);
        assertDepthAllowed(depthOf(targetParent) + 1 + subtreeHeight(all, source));

        // Plan limit: đếm đủ N diagram trong subtree TRƯỚC khi copy — không copy dở dang rồi mới chặn
        long baseCount = sheetRepository.countByProject_UserAndProject_IsDeletedFalse(project.getUser());
        long newDiagrams = subtreeItems(all, source).stream()
                .filter(i -> i.getKind() == WorkspaceItemKind.DIAGRAM).count();
        for (long i = 0; i < newDiagrams; i++) {
            planLimitService.assertCanCreate(project.getUser().getId(), PlanFeatureKey.MAX_DIAGRAMS, baseCount + i);
        }

        String rootName;
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            rootName = request.getName().trim();
            assertNameAvailable(all, targetParentId, rootName, null);
        } else {
            rootName = uniqueName(all, targetParentId, source.getName());
        }

        WorkspaceItem copy = copyRecursive(all, source, targetParent, rootName, user, project);
        placeAmongSiblings(all, copy, targetParentId, null);

        socketService.broadcastWorkspaceChanged(project.getId());
        log.info("Workspace item duplicated: {} -> {} in project {}", source.getId(), copy.getId(), project.getId());
        return ApiResponse.success("Nhân bản mục workspace thành công",
                workspaceItemMapper.toWorkspaceItemResponse(copy));
    }

    @Override
    public ApiResponse<Void> deleteItems(String email, WorkspaceItemDeleteRequest request) {
        List<UUID> ids = request.getIds().stream().distinct().toList();
        List<WorkspaceItem> selected = workspaceItemRepository.findAllById(ids);
        if (selected.size() != ids.size()) {
            throw new AppException(ErrorCode.WORKSPACE_ITEM_NOT_FOUND);
        }

        UUID projectId = selected.get(0).getProject().getId();
        if (selected.stream().anyMatch(i -> !i.getProject().getId().equals(projectId))) {
            throw new AppException(ErrorCode.WORKSPACE_ITEMS_SAME_PROJECT);
        }

        Project project = lockProject(projectId);
        projectAccessService.validateAccess(project, email);
        boolean recursive = Boolean.TRUE.equals(request.getRecursive());
        List<WorkspaceItem> all = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project);

        // Dedupe theo id — KHÔNG dùng Set<WorkspaceItem>: equals() của @Data không dựa trên id,
        // hai item khác nhau trùng name/kind/orderIndex sẽ bị coi là một và sót row khi xóa
        LinkedHashMap<UUID, WorkspaceItem> toDelete = new LinkedHashMap<>();
        for (WorkspaceItem root : selected) {
            List<WorkspaceItem> subtree = subtreeItems(all, root);
            if (root.getKind() == WorkspaceItemKind.FOLDER && subtree.size() > 1 && !recursive) {
                throw new AppException(ErrorCode.WORKSPACE_FOLDER_NOT_EMPTY);
            }
            subtree.forEach(item -> toDelete.putIfAbsent(item.getId(), item));
        }

        // Xóa item con trước cha (FK parent_id), flush xong mới xóa sheet (FK sheet_id trỏ từ item)
        List<WorkspaceItem> ordered = new ArrayList<>(toDelete.values());
        ordered.sort(Comparator.comparingInt(this::depthOf).reversed());
        List<Sheet> sheetsToDelete = ordered.stream()
                .map(WorkspaceItem::getSheet)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        workspaceItemRepository.deleteAll(ordered);
        workspaceItemRepository.flush();
        sheetRepository.deleteAll(sheetsToDelete);

        // Dồn lại orderIndex tại các folder bị mất con
        Set<UUID> deletedIds = ordered.stream().map(WorkspaceItem::getId).collect(Collectors.toSet());
        Set<String> handledParents = new HashSet<>();
        for (WorkspaceItem root : selected) {
            UUID parentId = idOf(root.getParent());
            if (!handledParents.add(String.valueOf(parentId))) continue;
            List<WorkspaceItem> remaining = childrenOf(all, parentId).stream()
                    .filter(child -> !deletedIds.contains(child.getId()))
                    .collect(Collectors.toList());
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setOrderIndex(i);
            }
            workspaceItemRepository.saveAll(remaining);
        }

        socketService.broadcastWorkspaceChanged(projectId);
        log.info("Deleted {} workspace item(s) ({} sheet(s)) in project {}",
                ordered.size(), sheetsToDelete.size(), projectId);
        return ApiResponse.success("Xóa các mục workspace thành công");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private WorkspaceItem getItem(UUID itemId) {
        return workspaceItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_ITEM_NOT_FOUND));
    }

    private Project lockProject(UUID projectId) {
        return projectRepository.findWithLockById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private WorkspaceItem resolveParent(List<WorkspaceItem> all, UUID parentId) {
        if (parentId == null) return null;
        WorkspaceItem parent = all.stream()
                .filter(item -> parentId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_PARENT_NOT_FOUND));
        if (parent.getKind() != WorkspaceItemKind.FOLDER) {
            throw new AppException(ErrorCode.WORKSPACE_PARENT_NOT_FOLDER);
        }
        return parent;
    }

    private static UUID idOf(WorkspaceItem item) {
        return item == null ? null : item.getId();
    }

    /** Các con trực tiếp của parentId (null = gốc project), sort theo orderIndex — list mutable. */
    private List<WorkspaceItem> childrenOf(List<WorkspaceItem> all, UUID parentId) {
        return all.stream()
                .filter(item -> Objects.equals(idOf(item.getParent()), parentId))
                .sorted(Comparator.comparingInt(item -> item.getOrderIndex() == null ? 0 : item.getOrderIndex()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Toàn bộ subtree (gồm chính root), thứ tự cha-trước-con. */
    private List<WorkspaceItem> subtreeItems(List<WorkspaceItem> all, WorkspaceItem root) {
        List<WorkspaceItem> result = new ArrayList<>();
        Deque<WorkspaceItem> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            WorkspaceItem current = queue.poll();
            result.add(current);
            queue.addAll(childrenOf(all, current.getId()));
        }
        return result;
    }

    /** Độ sâu của item tính từ gốc (null = 0, item ở gốc = 1). */
    private int depthOf(WorkspaceItem item) {
        int depth = 0;
        for (WorkspaceItem cursor = item; cursor != null; cursor = cursor.getParent()) depth++;
        return depth;
    }

    /** Chiều cao subtree dưới root (leaf = 0). */
    private int subtreeHeight(List<WorkspaceItem> all, WorkspaceItem root) {
        int max = 0;
        for (WorkspaceItem child : childrenOf(all, root.getId())) {
            max = Math.max(max, 1 + subtreeHeight(all, child));
        }
        return max;
    }

    private void assertDepthAllowed(int depth) {
        if (depth > MAX_DEPTH) {
            throw new AppException(ErrorCode.WORKSPACE_MAX_DEPTH_EXCEEDED);
        }
    }

    private void assertNameAvailable(List<WorkspaceItem> all, UUID parentId, String name, UUID excludeId) {
        boolean taken = childrenOf(all, parentId).stream()
                .anyMatch(item -> !item.getId().equals(excludeId) && item.getName().equalsIgnoreCase(name));
        if (taken) {
            throw new AppException(ErrorCode.WORKSPACE_DUPLICATE_NAME);
        }
    }

    private void assertMarkdownSize(String content) {
        if (content != null && content.getBytes(StandardCharsets.UTF_8).length > MAX_MARKDOWN_BYTES) {
            throw new AppException(ErrorCode.WORKSPACE_MARKDOWN_TOO_LARGE);
        }
    }

    private void assertVersionMatches(WorkspaceItem item, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(item.getVersion())) {
            throw new AppException(ErrorCode.WORKSPACE_VERSION_CONFLICT);
        }
    }

    /** Chèn item vào danh sách anh em của parentId tại vị trí yêu cầu (null = cuối) rồi đánh lại 0..n-1. */
    private void placeAmongSiblings(List<WorkspaceItem> all, WorkspaceItem item, UUID parentId, Integer requestedIndex) {
        List<WorkspaceItem> siblings = childrenOf(all, parentId);
        siblings.remove(item);
        int index = requestedIndex == null ? siblings.size()
                : Math.max(0, Math.min(requestedIndex, siblings.size()));
        siblings.add(index, item);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setOrderIndex(i);
        }
        workspaceItemRepository.saveAll(siblings);
    }

    /** Đánh lại orderIndex 0..n-1 cho các con của parentId, bỏ qua item vừa rời đi. */
    private void reindexChildren(List<WorkspaceItem> all, UUID parentId, WorkspaceItem exclude) {
        List<WorkspaceItem> children = childrenOf(all, parentId);
        children.remove(exclude);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).setOrderIndex(i);
        }
        workspaceItemRepository.saveAll(children);
    }

    /** Sinh tên không trùng trong folder đích: "Tên (2)", "Tên (3)"... — giữ đuôi .md nếu có. */
    private String uniqueName(List<WorkspaceItem> all, UUID parentId, String baseName) {
        Set<String> taken = childrenOf(all, parentId).stream()
                .map(item -> item.getName().toLowerCase())
                .collect(Collectors.toSet());
        if (!taken.contains(baseName.toLowerCase())) return baseName;

        String stem = baseName;
        String extension = "";
        if (baseName.toLowerCase().endsWith(".md")) {
            stem = baseName.substring(0, baseName.length() - 3);
            extension = baseName.substring(baseName.length() - 3);
        }
        int index = 2;
        while (taken.contains((stem + " (" + index + ")" + extension).toLowerCase())) index++;
        return stem + " (" + index + ")" + extension;
    }

    private Sheet createSheetForDiagram(Project project, String name, String requestedType, String diagramData) {
        planLimitService.assertCanCreate(project.getUser().getId(), PlanFeatureKey.MAX_DIAGRAMS,
                sheetRepository.countByProject_UserAndProject_IsDeletedFalse(project.getUser()));

        String fallback = (requestedType == null || requestedType.isBlank()) ? DEFAULT_DIAGRAM_TYPE : requestedType;
        String type = resolveDiagramType(diagramData, fallback);
        String data = (diagramData == null || diagramData.isBlank()) ? defaultDiagramData(type) : diagramData;

        Sheet sheet = sheetMapper.toSheet(name,
                sheetRepository.findAllByProjectOrderByOrderIndexAsc(project).size(), data, type, project);
        return sheetRepository.save(sheet);
    }

    private WorkspaceItem copyRecursive(List<WorkspaceItem> all, WorkspaceItem source, WorkspaceItem targetParent,
                                        String name, User user, Project project) {
        WorkspaceItem copy = workspaceItemMapper.copyItem(source, name, targetParent, project, user);
        copy.setOrderIndex(0);

        switch (source.getKind()) {
            case MARKDOWN -> {
                if (copy.getMarkdownContent() == null) copy.setMarkdownContent("");
            }
            case DIAGRAM -> {
                Sheet sourceSheet = source.getSheet();
                Sheet sheet = sheetMapper.toSheet(name,
                        sheetRepository.findAllByProjectOrderByOrderIndexAsc(project).size(),
                        sourceSheet != null ? sourceSheet.getDiagramData()
                                : defaultDiagramData(DEFAULT_DIAGRAM_TYPE),
                        sourceSheet != null && sourceSheet.getDiagramType() != null
                                ? sourceSheet.getDiagramType() : DEFAULT_DIAGRAM_TYPE,
                        project);
                copy.setSheet(sheetRepository.save(sheet));
            }
            case FOLDER -> { /* con được copy bên dưới */ }
        }

        WorkspaceItem saved = workspaceItemRepository.save(copy);

        if (source.getKind() == WorkspaceItemKind.FOLDER) {
            // Duyệt trên snapshot `all` (chưa chứa các bản copy) → không lặp vô hạn kể cả khi
            // đích nằm bên trong subtree nguồn
            int index = 0;
            for (WorkspaceItem child : childrenOf(all, source.getId())) {
                WorkspaceItem childCopy = copyRecursive(all, child, saved, child.getName(), user, project);
                childCopy.setOrderIndex(index++);
            }
        }
        return saved;
    }

    private String resolveDiagramType(String diagramData, String fallback) {
        if (diagramData == null || diagramData.isBlank()) return fallback;
        try {
            JsonNode node = objectMapper.readTree(diagramData);
            String type = node.path("diagramType").asText(null);
            return (type == null || type.isBlank()) ? fallback : type;
        } catch (Exception e) {
            log.warn("Không parse được diagramData để lấy diagramType, fallback '{}'", fallback);
            return fallback;
        }
    }

    private String defaultDiagramData(String diagramType) {
        return "{\"nodes\": [], \"edges\": [], \"diagramType\": \"" + diagramType
                + "\", \"viewport\": {\"x\": 0, \"y\": 0, \"zoom\": 1}}";
    }
}
