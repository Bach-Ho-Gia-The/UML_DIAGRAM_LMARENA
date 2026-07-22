package su26.uml.be.features.workspace.service;

import su26.uml.be.features.workspace.dto.WorkspaceItemCreateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemDeleteRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemDuplicateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemMoveRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemUpdateRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.workspace.dto.WorkspaceItemResponse;

import java.util.List;
import java.util.UUID;

public interface WorkspaceItemService {
    ApiResponse<List<WorkspaceItemResponse>> getWorkspaceItems(UUID projectId, String email);
    ApiResponse<WorkspaceItemResponse> createItem(UUID projectId, String email, WorkspaceItemCreateRequest request);
    ApiResponse<WorkspaceItemResponse> updateItem(UUID itemId, String email, WorkspaceItemUpdateRequest request);
    ApiResponse<WorkspaceItemResponse> moveItem(UUID itemId, String email, WorkspaceItemMoveRequest request);/** Nhân bản item (folder = đệ quy, diagram = tạo sheet mới copy data) — atomic. */
    ApiResponse<WorkspaceItemResponse> duplicateItem(UUID itemId, String email, WorkspaceItemDuplicateRequest request);
    ApiResponse<Void> deleteItems(String email, WorkspaceItemDeleteRequest request);
}