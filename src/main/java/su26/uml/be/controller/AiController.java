package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.dto.request.AiCreateWorkspaceRequest;
import su26.uml.be.dto.request.AiDocumentDeleteRequest;
import su26.uml.be.dto.request.AiSystemConfigRequest;
import su26.uml.be.dto.request.AiWorkspaceUpdateRequest;
import su26.uml.be.dto.request.ProviderModelsRequest;
import su26.uml.be.dto.response.*;
import su26.uml.be.service.AiService;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "AI Admin", description = "Admin AnythingLLM configuration.")
public class AiController {

    AiService aiService;

    @GetMapping("/config")
    @Operation(summary = "Get system config", description = "Get current LLM provider, model, vector DB settings from AnythingLLM.")
    public ApiResponse<AiSystemConfigResponse> getSystemConfig() {
        return aiService.getSystemConfig();
    }

    @PutMapping("/config")
    @Operation(summary = "Update system config", description = "Update LLM provider, model, vector DB settings on AnythingLLM.")
    public ApiResponse<AiSystemConfigResponse> updateSystemConfig(
            @Valid @RequestBody AiSystemConfigRequest request) {
        return aiService.updateSystemConfig(request);
    }

    @GetMapping("/providers")
    @Operation(summary = "Get supported providers", description = "Get list of supported LLM providers from AnythingLLM.")
    public ApiResponse<List<String>> getSupportedProviders() {
        return aiService.getSupportedProviders();
    }

    @PostMapping("/test")
    @Operation(summary = "Test connection", description = "Ping AnythingLLM to check connectivity.")
    public ApiResponse<AiTestConnectionResponse> testConnection() {
        return aiService.testConnection();
    }

    @GetMapping("/workspace")
    @Operation(summary = "Get workspace", description = "Get current workspace details including RAG settings. Optional ?slug= query param, defaults to configured workspace.")
    public ApiResponse<AiWorkspaceResponse> getWorkspace(
            @RequestParam(required = false) String slug) {
        return aiService.getWorkspaceBySlug(slug);
    }

    @PutMapping("/workspace")
    @Operation(summary = "Update workspace", description = "Update workspace model, chat mode, and RAG settings. Optional ?slug= query param, defaults to configured workspace.")
    public ApiResponse<AiWorkspaceResponse> updateWorkspace(
            @Valid @RequestBody AiWorkspaceUpdateRequest request,
            @RequestParam(required = false) String slug) {
        return aiService.updateWorkspace(request, slug);
    }

    @PostMapping("/providers/{provider}/models")
    @Operation(summary = "Get provider models", description = "Fetch available models for a given LLM provider.")
    public ApiResponse<List<String>> getProviderModels(
            @PathVariable String provider,
            @RequestBody(required = false) ProviderModelsRequest request) {
        String basePath = request != null ? request.getBasePath() : null;
        String apiKey = request != null ? request.getApiKey() : null;
        return aiService.getProviderModels(provider, basePath, apiKey);
    }

    @GetMapping("/workspaces")
    @Operation(summary = "List workspaces", description = "List all workspaces from AnythingLLM.")
    public ApiResponse<List<AiWorkspaceListItem>> getWorkspaces() {
        return aiService.getWorkspaces();
    }

    @GetMapping("/documents")
    @Operation(summary = "List documents", description = "List documents in a workspace. Optional ?workspace= query param, defaults to configured workspace.")
    public ApiResponse<List<AiDocumentResponse>> getDocuments(
            @RequestParam(required = false) String workspace) {
        return aiService.getDocuments(workspace);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document", description = "Upload a file to a workspace. Optional ?workspace= query param, defaults to configured workspace.")
    public ApiResponse<Void> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String workspace) {
        return aiService.uploadDocument(file, workspace);
    }

    @GetMapping("/documents/content")
    @Operation(summary = "Get document content", description = "Get the raw text content of a document by workspace and filename.")
    public ApiResponse<String> getDocumentContent(
            @RequestParam String workspace,
            @RequestParam String filename) {
        return aiService.getDocumentContent(workspace, filename);
    }

    @DeleteMapping("/documents")
    @Operation(summary = "Delete document", description = "Delete a document from the system by its document name.")
    public ApiResponse<Void> deleteDocument(@Valid @RequestBody AiDocumentDeleteRequest request) {
        return aiService.deleteDocument(request);
    }

    @GetMapping("/version")
    @Operation(summary = "Get AI version info", description = "Get AnythingLLM version, current LLM provider, model, and environment.")
    public ApiResponse<AiVersionResponse> getVersion() {
        return aiService.getVersion();
    }

    @PostMapping("/documents/re-embed")
    @Operation(summary = "Re-embed documents", description = "Re-embed documents in a workspace. Optional ?workspace= query param, defaults to configured workspace.")
    public ApiResponse<Void> reEmbedDocuments(
            @RequestParam(required = false) String workspace) {
        return aiService.reEmbedDocuments(workspace);
    }

    @PostMapping("/workspace")
    @Operation(summary = "Create workspace", description = "Create a new workspace in AnythingLLM.")
    public ApiResponse<Void> createWorkspace(
            @Valid @RequestBody AiCreateWorkspaceRequest request) {
        return aiService.createWorkspace(request);
    }

    @DeleteMapping("/workspace/{slug}")
    @Operation(summary = "Delete workspace", description = "Delete a workspace by slug from AnythingLLM.")
    public ApiResponse<Void> deleteWorkspace(@PathVariable String slug) {
        return aiService.deleteWorkspace(slug);
    }
}
