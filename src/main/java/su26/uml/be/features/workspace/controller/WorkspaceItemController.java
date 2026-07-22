package su26.uml.be.features.workspace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.common.config.SwaggerExamples;
import su26.uml.be.features.workspace.dto.WorkspaceItemCreateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemDeleteRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemDuplicateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemMoveRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemUpdateRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.workspace.dto.WorkspaceItemResponse;
import su26.uml.be.features.admin.service.ActivityTrackerService;
import su26.uml.be.features.workspace.service.WorkspaceItemService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Workspace Items", description = "Project workspace file tree APIs (folders, Markdown, diagrams)")
public class WorkspaceItemController {

    WorkspaceItemService workspaceItemService;
    ActivityTrackerService activityTracker;

    @GetMapping("/projects/{projectId}/workspace-items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "List workspace items",
            description = "Returns the flat list of all workspace items of a project; the client builds the tree from parentId.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Workspace items returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.WORKSPACE_ITEM_LIST_RESPONSE)))
    public ApiResponse<List<WorkspaceItemResponse>> getWorkspaceItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        return workspaceItemService.getWorkspaceItems(projectId, userDetails.getUsername());
    }

    @PostMapping("/projects/{projectId}/workspace-items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a workspace item",
            description = "Creates a FOLDER, MARKDOWN file, or DIAGRAM. For DIAGRAM the backing sheet is created atomically in the same transaction.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Workspace item created.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.WORKSPACE_ITEM_RESPONSE)))
    public ApiResponse<WorkspaceItemResponse> createItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @Valid @RequestBody WorkspaceItemCreateRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return workspaceItemService.createItem(projectId, userDetails.getUsername(), request);
    }

    @PatchMapping("/workspace-items/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update a workspace item",
            description = "Partial update: rename any item and/or update Markdown content. Only supplied fields are changed.")
    public ApiResponse<WorkspaceItemResponse> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId,
            @Valid @RequestBody WorkspaceItemUpdateRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return workspaceItemService.updateItem(itemId, userDetails.getUsername(), request);
    }

    @PatchMapping("/workspace-items/{itemId}/position")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Move / reorder a workspace item",
            description = "Moves the item to another folder (parentId null = project root) and/or reorders it. Sibling order indexes are normalized in one transaction; self/descendant targets are rejected.")
    public ApiResponse<WorkspaceItemResponse> moveItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId,
            @Valid @RequestBody WorkspaceItemMoveRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return workspaceItemService.moveItem(itemId, userDetails.getUsername(), request);
    }

    @PostMapping("/workspace-items/{itemId}/duplicate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Duplicate a workspace item",
            description = "Duplicates the item: Markdown copies its content, a diagram gets a new sheet with copied data, a folder is duplicated recursively — all atomic.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Workspace item duplicated.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.WORKSPACE_ITEM_RESPONSE)))
    public ApiResponse<WorkspaceItemResponse> duplicateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId,
            @Valid @RequestBody WorkspaceItemDuplicateRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return workspaceItemService.duplicateItem(itemId, userDetails.getUsername(), request);
    }

    @DeleteMapping("/workspace-items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete workspace items",
            description = "Deletes one or more items in one transaction. A non-empty folder requires recursive=true; linked sheets of deleted diagrams are removed as well.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Workspace items deleted.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.DELETE_WORKSPACE_ITEMS_RESPONSE)))
    public ApiResponse<Void> deleteItems(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WorkspaceItemDeleteRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return workspaceItemService.deleteItems(userDetails.getUsername(), request);
    }
}