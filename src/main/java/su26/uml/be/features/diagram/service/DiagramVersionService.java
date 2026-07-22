package su26.uml.be.features.diagram.service;

import su26.uml.be.features.diagram.dto.DiagramVersionCreateRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.diagram.dto.DiagramVersionResponse;

import java.util.List;
import java.util.UUID;

public interface DiagramVersionService {
    ApiResponse<List<DiagramVersionResponse>> getVersions(String email, UUID sheetId);
    ApiResponse<DiagramVersionResponse> getVersion(String email, UUID sheetId, UUID versionId);
    ApiResponse<DiagramVersionResponse> createVersion(String email, UUID sheetId, DiagramVersionCreateRequest request);
    ApiResponse<DiagramVersionResponse> restoreVersion(String email, UUID sheetId, UUID versionId);
}