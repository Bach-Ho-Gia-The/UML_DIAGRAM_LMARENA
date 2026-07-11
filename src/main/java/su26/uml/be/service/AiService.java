package su26.uml.be.service;

import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.dto.request.AiCreateWorkspaceRequest;
import su26.uml.be.dto.request.AiDocumentDeleteRequest;
import su26.uml.be.dto.request.AiSystemConfigRequest;
import su26.uml.be.dto.request.AiWorkspaceUpdateRequest;
import su26.uml.be.dto.response.*;

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
