package su26.uml.be.features.project.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.project.dto.SheetRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.project.dto.SheetResponse;
import su26.uml.be.features.project.entity.Project;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.workspace.entity.WorkspaceItem;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.features.project.mapper.SheetMapper;
import su26.uml.be.features.workspace.mapper.WorkspaceItemMapper;
import su26.uml.be.features.diagram.repository.DiagramVersionRepository;
import su26.uml.be.features.project.repository.ProjectRepository;
import su26.uml.be.features.project.repository.SheetRepository;
import su26.uml.be.features.workspace.repository.WorkspaceItemRepository;
import su26.uml.be.features.plan.service.PlanLimitService;
import su26.uml.be.features.workspace.service.ProjectAccessService;
import su26.uml.be.features.project.service.SheetService;
import su26.uml.be.infrastructure.socket.SocketService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class SheetServiceImpl implements SheetService {

    static final String DEFAULT_DIAGRAM_TYPE = "activity";

    SheetRepository sheetRepository;
    ProjectRepository projectRepository;
    WorkspaceItemRepository workspaceItemRepository;
    DiagramVersionRepository diagramVersionRepository;
    SheetMapper sheetMapper;
    WorkspaceItemMapper workspaceItemMapper;
    PlanLimitService planLimitService;
    ProjectAccessService projectAccessService;
    SocketService socketService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiResponse<SheetResponse> createSheet(String email, SheetRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        User user = projectAccessService.validateAccess(project, email);

        planLimitService.assertCanCreate(project.getUser().getId(), PlanFeatureKey.MAX_DIAGRAMS,
                sheetRepository.countByProject_UserAndProject_IsDeletedFalse(project.getUser()));

        Sheet sheet = sheetMapper.toSheet(request);
        sheet.setProject(project);

        // Nếu chưa có diagramData, khởi tạo JSON trống cho Canvas
        if (sheet.getDiagramData() == null || sheet.getDiagramData().isEmpty()) {
            sheet.setDiagramData("{\"nodes\": [], \"edges\": []}");
        }
        sheet.setDiagramType(resolveDiagramType(sheet.getDiagramData(), DEFAULT_DIAGRAM_TYPE));

        Sheet savedSheet = sheetRepository.save(sheet);

        // Compat hook: sheet tạo qua API cũ cũng phải có DIAGRAM item ở gốc cây workspace (cùng tx)
        ensureWorkspaceItemForSheet(savedSheet, project, user);
        socketService.broadcastWorkspaceChanged(project.getId());

        log.info("Sheet created: {} for project: {}", savedSheet.getId(), project.getId());
        return ApiResponse.success("Tạo trang biểu đồ thành công", sheetMapper.toSheetResponse(savedSheet));
    }

    @Override
    public ApiResponse<SheetResponse> updateSheet(String email, UUID sheetId, SheetRequest request) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));
        projectAccessService.validateAccess(sheet.getProject(), email);

        String oldName = sheet.getName();
        sheetMapper.updateSheet(request, sheet);

        if (request.getDiagramData() != null) {
            sheet.setDiagramType(resolveDiagramType(sheet.getDiagramData(),
                    sheet.getDiagramType() != null ? sheet.getDiagramType() : DEFAULT_DIAGRAM_TYPE));
        }

        Sheet updatedSheet = sheetRepository.save(sheet);

        // Compat hook: tên sheet là canonical — đổi tên qua API cũ phải sync sang workspace item
        if (!Objects.equals(oldName, updatedSheet.getName())) {
            workspaceItemRepository.findBySheet(updatedSheet).ifPresent(item -> {
                item.setName(dedupeSiblingName(item, updatedSheet.getName()));
                workspaceItemRepository.save(item);
            });
            socketService.broadcastWorkspaceChanged(sheet.getProject().getId());
        }

        return ApiResponse.success("Cập nhật trang biểu đồ thành công", sheetMapper.toSheetResponse(updatedSheet));
    }

    @Override
    public ApiResponse<Void> deleteSheet(String email, UUID sheetId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));
        projectAccessService.validateAccess(sheet.getProject(), email);

        // Compat hook: xóa item trước (FK sheet_id) rồi mới xóa sheet — cùng tx
        workspaceItemRepository.findBySheet(sheet).ifPresent(item -> {
            workspaceItemRepository.delete(item);
            workspaceItemRepository.flush();
        });

        // Xóa lịch sử version trước (FK diagram_versions.sheet_id NOT NULL, không cascade)
        diagramVersionRepository.deleteAllBySheetIn(List.of(sheet));

        sheetRepository.delete(sheet);
        socketService.broadcastWorkspaceChanged(sheet.getProject().getId());
        log.info("Sheet deleted: {}", sheetId);
        return ApiResponse.success("Xóa trang biểu đồ thành công");
    }

    @Override
    public ApiResponse<SheetResponse> getSheetById(String email, UUID sheetId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));
        projectAccessService.validateAccess(sheet.getProject(), email);
        return ApiResponse.success("Lấy thông tin trang biểu đồ thành công", sheetMapper.toSheetResponse(sheet));
    }

    @Override
    public ApiResponse<List<SheetResponse>> getSheetsByProject(String email, UUID projectId) {
        Project project = projectAccessService.getProjectAndValidateAccess(projectId, email);
        List<Sheet> sheets = sheetRepository.findAllByProjectOrderByOrderIndexAsc(project);
        return ApiResponse.success("Lấy danh sách trang biểu đồ thành công", sheetMapper.toSheetResponseList(sheets));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Đảm bảo sheet có đúng 1 DIAGRAM item ở gốc cây (idempotent nhờ unique sheet_id). */
    private void ensureWorkspaceItemForSheet(Sheet sheet, Project project, User user) {
        if (workspaceItemRepository.existsBySheet(sheet)) return;

        List<WorkspaceItem> rootItems = workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project)
                .stream().filter(item -> item.getParent() == null).toList();
        Set<String> usedNames = rootItems.stream()
                .map(item -> item.getName().toLowerCase())
                .collect(Collectors.toSet());

        String name = sheet.getName();
        int index = 2;
        while (usedNames.contains(name.toLowerCase())) {
            name = sheet.getName() + " (" + index++ + ")";
        }

        workspaceItemRepository.save(
                workspaceItemMapper.toDiagramItem(sheet, name, rootItems.size(), project, user));
    }

    /** Tên item phải unique trong folder — API sheet cũ không được fail vì trùng, nên tự thêm suffix. */
    private String dedupeSiblingName(WorkspaceItem item, String desiredName) {
        UUID parentId = item.getParent() == null ? null : item.getParent().getId();
        Set<String> usedNames = workspaceItemRepository
                .findAllByProjectOrderByOrderIndexAsc(item.getProject()).stream()
                .filter(sibling -> !sibling.getId().equals(item.getId()))
                .filter(sibling -> Objects.equals(
                        sibling.getParent() == null ? null : sibling.getParent().getId(), parentId))
                .map(sibling -> sibling.getName().toLowerCase())
                .collect(Collectors.toSet());

        String name = desiredName;
        int index = 2;
        while (usedNames.contains(name.toLowerCase())) {
            name = desiredName + " (" + index++ + ")";
        }
        return name;
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
}