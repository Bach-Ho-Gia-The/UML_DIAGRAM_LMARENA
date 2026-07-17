package su26.uml.be.service;

import su26.uml.be.dto.request.SheetRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.SheetResponse;

import java.util.List;
import java.util.UUID;

public interface SheetService {
    ApiResponse<SheetResponse> createSheet(String email, SheetRequest request);
    ApiResponse<SheetResponse> updateSheet(String email, UUID sheetId, SheetRequest request);
    ApiResponse<Void> deleteSheet(String email, UUID sheetId);
    ApiResponse<SheetResponse> getSheetById(String email, UUID sheetId);
    ApiResponse<List<SheetResponse>> getSheetsByProject(String email, UUID projectId);
}
