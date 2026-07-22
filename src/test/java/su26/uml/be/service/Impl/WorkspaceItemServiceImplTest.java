package su26.uml.be.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import su26.uml.be.features.project.entity.Project;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.features.project.mapper.SheetMapper;
import su26.uml.be.features.project.repository.ProjectRepository;
import su26.uml.be.features.project.repository.SheetRepository;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.workspace.dto.WorkspaceItemCreateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemDeleteRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemMoveRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemUpdateRequest;
import su26.uml.be.features.workspace.entity.WorkspaceItem;
import su26.uml.be.features.workspace.mapper.WorkspaceItemMapper;
import su26.uml.be.features.workspace.repository.WorkspaceItemRepository;
import su26.uml.be.features.workspace.service.Impl.WorkspaceItemServiceImpl;
import su26.uml.be.features.workspace.service.ProjectAccessService;
import su26.uml.be.features.plan.service.PlanLimitService;
import su26.uml.be.infrastructure.socket.SocketService;
import su26.uml.be.features.diagram.repository.DiagramVersionRepository;
import su26.uml.be.common.constant.enums.WorkspaceItemKind;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceItemServiceImplTest {

    private static final String EMAIL = "owner@gmail.com";

    @Mock WorkspaceItemRepository workspaceItemRepository;
    @Mock ProjectRepository projectRepository;
    @Mock SheetRepository sheetRepository;
    @Mock WorkspaceItemMapper workspaceItemMapper;
    @Mock SheetMapper sheetMapper;
    @Mock ProjectAccessService projectAccessService;
    @Mock PlanLimitService planLimitService;
    @Mock SocketService socketService;
    @Mock DiagramVersionRepository diagramVersionRepository;

    @InjectMocks WorkspaceItemServiceImpl service;

    private Project project;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().email(EMAIL).build();
        project = Project.builder().projectName("Demo").user(owner).build();
        project.setId(UUID.randomUUID());

        when(projectRepository.findWithLockById(project.getId())).thenReturn(Optional.of(project));
        when(projectAccessService.validateAccess(project, EMAIL)).thenReturn(owner);
    }

    private WorkspaceItem item(String name, WorkspaceItemKind kind, WorkspaceItem parent) {
        WorkspaceItem item = WorkspaceItem.builder()
                .name(name)
                .kind(kind)
                .orderIndex(0)
                .project(project)
                .parent(parent)
                .build();
        item.setId(UUID.randomUUID());
        return item;
    }

    private void treeIs(WorkspaceItem... items) {
        when(workspaceItemRepository.findAllByProjectOrderByOrderIndexAsc(project)).thenReturn(List.of(items));
        for (WorkspaceItem item : items) {
            when(workspaceItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        }
        when(workspaceItemRepository.findAllById(anyList())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return List.of(items).stream().filter(i -> ids.contains(i.getId())).toList();
        });
    }

    private ErrorCode errorOf(Runnable action) {
        AppException exception = assertThrows(AppException.class, action::run);
        return exception.getErrorCode();
    }

    // ─── Move: cycle ──────────────────────────────────────────────────────────

    @Test
    void moveItem_intoItself_throwsTreeCycle() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        treeIs(folderA);

        ErrorCode error = errorOf(() -> service.moveItem(folderA.getId(), EMAIL,
                WorkspaceItemMoveRequest.builder().parentId(folderA.getId()).build()));

        assertEquals(ErrorCode.WORKSPACE_TREE_CYCLE, error);
    }

    @Test
    void moveItem_intoOwnDescendant_throwsTreeCycle() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        WorkspaceItem folderB = item("B", WorkspaceItemKind.FOLDER, folderA);
        treeIs(folderA, folderB);

        ErrorCode error = errorOf(() -> service.moveItem(folderA.getId(), EMAIL,
                WorkspaceItemMoveRequest.builder().parentId(folderB.getId()).build()));

        assertEquals(ErrorCode.WORKSPACE_TREE_CYCLE, error);
    }

    @Test
    void moveItem_intoSiblingFolder_reparentsAndNormalizesOrder() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        WorkspaceItem markdown = item("notes.md", WorkspaceItemKind.MARKDOWN, null);
        markdown.setOrderIndex(1);
        treeIs(folderA, markdown);

        service.moveItem(markdown.getId(), EMAIL,
                WorkspaceItemMoveRequest.builder().parentId(folderA.getId()).build());

        assertEquals(folderA, markdown.getParent());
        assertEquals(0, markdown.getOrderIndex());
        verify(workspaceItemRepository, atLeastOnce()).saveAll(anyList());
    }

    // ─── Create: duplicate name / parent kind ─────────────────────────────────

    @Test
    void createItem_duplicateSiblingName_caseInsensitive_throwsDuplicateName() {
        WorkspaceItem existing = item("Notes.md", WorkspaceItemKind.MARKDOWN, null);
        treeIs(existing);

        ErrorCode error = errorOf(() -> service.createItem(project.getId(), EMAIL,
                WorkspaceItemCreateRequest.builder()
                        .name("notes.MD").kind(WorkspaceItemKind.MARKDOWN).build()));

        assertEquals(ErrorCode.WORKSPACE_DUPLICATE_NAME, error);
    }

    @Test
    void createItem_sameNameInDifferentFolder_isAllowed() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        WorkspaceItem existing = item("Notes.md", WorkspaceItemKind.MARKDOWN, null);
        treeIs(folderA, existing);
        when(workspaceItemMapper.toWorkspaceItem(any(WorkspaceItemCreateRequest.class)))
                .thenAnswer(invocation -> {
                    WorkspaceItemCreateRequest request = invocation.getArgument(0);
                    return WorkspaceItem.builder()
                            .name(request.getName()).kind(request.getKind())
                            .markdownContent(request.getContent()).build();
                });

        assertDoesNotThrow(() -> service.createItem(project.getId(), EMAIL,
                WorkspaceItemCreateRequest.builder()
                        .name("Notes.md").kind(WorkspaceItemKind.MARKDOWN)
                        .parentId(folderA.getId()).build()));
    }

    @Test
    void createItem_parentIsMarkdownFile_throwsParentNotFolder() {
        WorkspaceItem markdown = item("notes.md", WorkspaceItemKind.MARKDOWN, null);
        treeIs(markdown);

        ErrorCode error = errorOf(() -> service.createItem(project.getId(), EMAIL,
                WorkspaceItemCreateRequest.builder()
                        .name("Child").kind(WorkspaceItemKind.FOLDER)
                        .parentId(markdown.getId()).build()));

        assertEquals(ErrorCode.WORKSPACE_PARENT_NOT_FOLDER, error);
    }

    @Test
    void createItem_contentOnFolder_throwsContentNotAllowed() {
        treeIs();

        ErrorCode error = errorOf(() -> service.createItem(project.getId(), EMAIL,
                WorkspaceItemCreateRequest.builder()
                        .name("Docs").kind(WorkspaceItemKind.FOLDER).content("# nope").build()));

        assertEquals(ErrorCode.WORKSPACE_CONTENT_NOT_ALLOWED, error);
    }

    // ─── Update: content guard / optimistic version ───────────────────────────

    @Test
    void updateItem_contentOnDiagram_throwsContentNotAllowed() {
        WorkspaceItem diagram = item("Flow", WorkspaceItemKind.DIAGRAM, null);
        treeIs(diagram);

        ErrorCode error = errorOf(() -> service.updateItem(diagram.getId(), EMAIL,
                WorkspaceItemUpdateRequest.builder().content("# text").build()));

        assertEquals(ErrorCode.WORKSPACE_CONTENT_NOT_ALLOWED, error);
    }

    @Test
    void updateItem_staleExpectedVersion_throwsVersionConflict() {
        WorkspaceItem markdown = item("notes.md", WorkspaceItemKind.MARKDOWN, null);
        markdown.setVersion(5L);
        treeIs(markdown);

        ErrorCode error = errorOf(() -> service.updateItem(markdown.getId(), EMAIL,
                WorkspaceItemUpdateRequest.builder().content("# new").expectedVersion(4L).build()));

        assertEquals(ErrorCode.WORKSPACE_VERSION_CONFLICT, error);
    }

    // ─── Delete: folder-not-empty / recursive ─────────────────────────────────

    @Test
    void deleteItems_nonEmptyFolderWithoutRecursive_throwsFolderNotEmpty() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        WorkspaceItem child = item("notes.md", WorkspaceItemKind.MARKDOWN, folderA);
        treeIs(folderA, child);

        ErrorCode error = errorOf(() -> service.deleteItems(EMAIL,
                WorkspaceItemDeleteRequest.builder().ids(List.of(folderA.getId())).build()));

        assertEquals(ErrorCode.WORKSPACE_FOLDER_NOT_EMPTY, error);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteItems_recursive_deletesWholeSubtreeChildrenFirstAndLinkedSheets() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        WorkspaceItem diagram = item("Flow", WorkspaceItemKind.DIAGRAM, folderA);
        Sheet sheet = Sheet.builder().name("Flow").project(project).build();
        sheet.setId(UUID.randomUUID());
        diagram.setSheet(sheet);
        WorkspaceItem markdown = item("notes.md", WorkspaceItemKind.MARKDOWN, folderA);
        treeIs(folderA, diagram, markdown);

        service.deleteItems(EMAIL, WorkspaceItemDeleteRequest.builder()
                .ids(List.of(folderA.getId())).recursive(true).build());

        ArgumentCaptor<List<WorkspaceItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(workspaceItemRepository).deleteAll(itemsCaptor.capture());
        List<WorkspaceItem> deleted = itemsCaptor.getValue();
        assertEquals(3, deleted.size());
        // Con phải bị xóa trước cha (FK parent_id)
        assertTrue(deleted.indexOf(diagram) < deleted.indexOf(folderA));
        assertTrue(deleted.indexOf(markdown) < deleted.indexOf(folderA));

        ArgumentCaptor<List<Sheet>> sheetsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sheetRepository).deleteAll(sheetsCaptor.capture());
        assertEquals(List.of(sheet), sheetsCaptor.getValue());
    }

    @Test
    void deleteItems_emptyFolderWithoutRecursive_succeeds() {
        WorkspaceItem folderA = item("A", WorkspaceItemKind.FOLDER, null);
        treeIs(folderA);

        assertDoesNotThrow(() -> service.deleteItems(EMAIL,
                WorkspaceItemDeleteRequest.builder().ids(List.of(folderA.getId())).build()));
        verify(workspaceItemRepository).deleteAll(List.of(folderA));
    }
}
