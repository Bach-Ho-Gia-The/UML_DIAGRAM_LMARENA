package su26.uml.be.service;

import su26.uml.be.dto.request.WorkspaceItemCreateRequest;
import su26.uml.be.dto.request.WorkspaceItemDeleteRequest;
import su26.uml.be.dto.request.WorkspaceItemDuplicateRequest;
import su26.uml.be.dto.request.WorkspaceItemMoveRequest;
import su26.uml.be.dto.request.WorkspaceItemUpdateRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.WorkspaceItemResponse;

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
