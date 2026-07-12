package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.dto.request.SheetRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.SheetResponse;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.Sheet;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.enums.PlanFeatureKey;
import su26.uml.be.mapper.SheetMapper;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.repository.SheetRepository;
import su26.uml.be.service.PlanLimitService;
import su26.uml.be.service.SheetService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class SheetServiceImpl implements SheetService {

    SheetRepository sheetRepository;
    ProjectRepository projectRepository;
    SheetMapper sheetMapper;
    PlanLimitService planLimitService;

    @Override
    public ApiResponse<SheetResponse> createSheet(SheetRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        planLimitService.assertCanCreate(project.getUser().getId(), PlanFeatureKey.MAX_DIAGRAMS,
                sheetRepository.countByProject_UserAndProject_IsDeletedFalse(project.getUser()));

        Sheet sheet = sheetMapper.toSheet(request);
        sheet.setProject(project);

        // Nếu chưa có diagramData, khởi tạo JSON trống cho Canvas
        if (sheet.getDiagramData() == null || sheet.getDiagramData().isEmpty()) {
            sheet.setDiagramData("{\"nodes\": [], \"edges\": []}");
        }

        Sheet savedSheet = sheetRepository.save(sheet);
        log.info("Sheet created: {} for project: {}", savedSheet.getId(), project.getId());
        return ApiResponse.success("Tạo trang biểu đồ thành công", sheetMapper.toSheetResponse(savedSheet));
    }

    @Override
    public ApiResponse<SheetResponse> updateSheet(UUID sheetId, SheetRequest request) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));

        sheetMapper.updateSheet(request, sheet);
        Sheet updatedSheet = sheetRepository.save(sheet);
        return ApiResponse.success("Cập nhật trang biểu đồ thành công", sheetMapper.toSheetResponse(updatedSheet));
    }

    @Override
    public ApiResponse<Void> deleteSheet(UUID sheetId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));

        sheetRepository.delete(sheet);
        log.info("Sheet deleted: {}", sheetId);
        return ApiResponse.success("Xóa trang biểu đồ thành công");
    }

    @Override
    public ApiResponse<SheetResponse> getSheetById(UUID sheetId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));
        return ApiResponse.success("Lấy thông tin trang biểu đồ thành công", sheetMapper.toSheetResponse(sheet));
    }

    @Override
    public ApiResponse<List<SheetResponse>> getSheetsByProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        List<Sheet> sheets = sheetRepository.findAllByProjectOrderByOrderIndexAsc(project);
        return ApiResponse.success("Lấy danh sách trang biểu đồ thành công", sheetMapper.toSheetResponseList(sheets));
    }
}
