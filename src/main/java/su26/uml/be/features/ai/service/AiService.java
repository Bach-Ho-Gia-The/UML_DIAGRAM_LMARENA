package su26.uml.be.features.ai.service;

import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.features.ai.dto.AiCreateWorkspaceRequest;
import su26.uml.be.features.ai.dto.AiDocumentDeleteRequest;
import su26.uml.be.features.ai.dto.AiSystemConfigRequest;
import su26.uml.be.features.ai.dto.AiWorkspaceUpdateRequest;
import su26.uml.be.features.ai.dto.*;
import su26.uml.be.common.response.ApiResponse;

import java.util.List;

public interface AiService {

    ApiResponse<AiSystemConfigResponse> getSystemConfig();

    ApiResponse<AiSystemConfigResponse> updateSystemConfig(AiSystemConfigRequest request);

    ApiResponse<List<String>> getSupportedProviders();

    ApiResponse<AiTestConnectionResponse> testConnection();

    ApiResponse<AiWorkspaceResponse> getWorkspaceBySlug(String slug);

    ApiResponse<AiWorkspaceResponse> updateWorkspace(AiWorkspaceUpdateRequest request, String slug);

    ApiResponse<List<String>> getProviderModels(String provider, String basePath, String apiKey);

    ApiResponse<List<AiWorkspaceListItem>> getWorkspaces();

    ApiResponse<List<AiDocumentResponse>> getDocuments(String workspaceSlug);

    ApiResponse<Void> uploadDocument(MultipartFile file, String workspaceSlug);

    ApiResponse<String> getDocumentContent(String workspace, String filename);

    ApiResponse<Void> deleteDocument(AiDocumentDeleteRequest request);

    ApiResponse<Void> reEmbedDocuments(String workspaceSlug);

    ApiResponse<AiVersionResponse> getVersion();

    ApiResponse<Void> createWorkspace(AiCreateWorkspaceRequest request);

    ApiResponse<Void> deleteWorkspace(String slug);
}