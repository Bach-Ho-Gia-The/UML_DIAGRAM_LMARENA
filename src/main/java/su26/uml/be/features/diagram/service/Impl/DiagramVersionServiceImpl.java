package su26.uml.be.features.diagram.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.features.diagram.dto.DiagramVersionCreateRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.diagram.dto.DiagramVersionResponse;
import su26.uml.be.features.diagram.entity.DiagramVersion;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.DiagramVersionSource;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.diagram.mapper.DiagramVersionMapper;
import su26.uml.be.features.diagram.repository.DiagramVersionRepository;
import su26.uml.be.features.project.repository.SheetRepository;
import su26.uml.be.features.diagram.service.DiagramVersionService;
import su26.uml.be.features.workspace.service.ProjectAccessService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class DiagramVersionServiceImpl implements DiagramVersionService {

    static final int MAX_SNAPSHOT_BYTES = 2 * 1024 * 1024;
    static final int MAX_AUTO_VERSIONS_PER_SHEET = 50;
    static final int DEFAULT_SCHEMA_VERSION = 1;

    DiagramVersionRepository diagramVersionRepository;
    SheetRepository sheetRepository;
    DiagramVersionMapper diagramVersionMapper;
    ProjectAccessService projectAccessService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<DiagramVersionResponse>> getVersions(String email, UUID sheetId) {
        Sheet sheet = getSheetAndValidateAccess(email, sheetId);
        List<DiagramVersion> versions = diagramVersionRepository.findAllBySheetOrderByVersionNumberDesc(sheet);
        return ApiResponse.success("Lấy danh sách phiên bản sơ đồ thành công",
                diagramVersionMapper.toDiagramVersionSummaryList(versions));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DiagramVersionResponse> getVersion(String email, UUID sheetId, UUID versionId) {
        Sheet sheet = getSheetAndValidateAccess(email, sheetId);
        DiagramVersion version = getVersionOfSheet(versionId, sheet);
        return ApiResponse.success("Lấy chi tiết phiên bản sơ đồ thành công",
                diagramVersionMapper.toDiagramVersionResponse(version));
    }

    @Override
    public ApiResponse<DiagramVersionResponse> createVersion(String email, UUID sheetId,
                                                             DiagramVersionCreateRequest request) {
        Sheet sheet = getSheetAndValidateAccess(email, sheetId);
        User user = projectAccessService.validateAccess(sheet.getProject(), email);

        JsonNode snapshot = parseSnapshot(request.getDiagramData());
        String contentHash = sha256(request.getDiagramData());

        // Dedupe: không force và nội dung không đổi so với một version đã có → không tạo bản ghi mới
        if (!Boolean.TRUE.equals(request.getForce())) {
            var existing = diagramVersionRepository
                    .findFirstBySheetAndContentHashOrderByVersionNumberDesc(sheet, contentHash);
            if (existing.isPresent()) {
                return ApiResponse.success("Nội dung không đổi — dùng lại phiên bản đã có",
                        diagramVersionMapper.toDiagramVersionResponse(existing.get()));
            }
        }

        DiagramVersion version = buildVersion(sheet, user, request.getSource(),
                request.getName(), request.getNote(), request.getDiagramData(), contentHash,
                snapshot.path("schemaVersion").asInt(DEFAULT_SCHEMA_VERSION), null);
        DiagramVersion saved = diagramVersionRepository.save(version);

        pruneAutoVersions(sheet);

        log.info("Diagram version v{} ({}) created for sheet {}", saved.getVersionNumber(),
                saved.getSource(), sheetId);
        return ApiResponse.success("Tạo phiên bản sơ đồ thành công",
                diagramVersionMapper.toDiagramVersionResponse(saved));
    }

    @Override
    public ApiResponse<DiagramVersionResponse> restoreVersion(String email, UUID sheetId, UUID versionId) {
        Sheet sheet = getSheetAndValidateAccess(email, sheetId);
        User user = projectAccessService.validateAccess(sheet.getProject(), email);
        DiagramVersion target = getVersionOfSheet(versionId, sheet);

        // Validate snapshot lịch sử trước khi ghi đè canvas hiện tại
        JsonNode snapshot = parseSnapshot(target.getDiagramData());

        // 1. Backup trạng thái hiện tại (BEFORE_RESTORE) — lịch sử không bao giờ bị mất
        String currentData = sheet.getDiagramData() == null ? "{\"nodes\": [], \"edges\": []}" : sheet.getDiagramData();
        DiagramVersion backup = buildVersion(sheet, user, DiagramVersionSource.BEFORE_RESTORE,
                "Backup before restore", null, currentData, sha256(currentData),
                DEFAULT_SCHEMA_VERSION, null);
        diagramVersionRepository.save(backup);

        // 2. Ghi snapshot được chọn vào sheet (canonical canvas hiện tại)
        sheet.setDiagramData(target.getDiagramData());
        String restoredType = snapshot.path("diagramType").asText(null);
        if (restoredType != null && !restoredType.isBlank()) {
            sheet.setDiagramType(restoredType);
        }
        sheetRepository.save(sheet);

        // 3. Bản ghi RESTORE trỏ về version gốc (audit) — không mutate row lịch sử nào
        DiagramVersion restored = buildVersion(sheet, user, DiagramVersionSource.RESTORE,
                "Restored from " + target.getName(), null, target.getDiagramData(),
                target.getContentHash(),
                target.getSchemaVersion() == null ? DEFAULT_SCHEMA_VERSION : target.getSchemaVersion(),
                target);
        DiagramVersion saved = diagramVersionRepository.save(restored);

        log.info("Sheet {} restored from version v{} by {}", sheetId, target.getVersionNumber(), email);
        return ApiResponse.success("Khôi phục phiên bản thành công",
                diagramVersionMapper.toDiagramVersionResponse(saved));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Sheet getSheetAndValidateAccess(String email, UUID sheetId) {
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new AppException(ErrorCode.SHEET_NOT_FOUND));
        projectAccessService.validateAccess(sheet.getProject(), email);
        return sheet;
    }

    private DiagramVersion getVersionOfSheet(UUID versionId, Sheet sheet) {
        DiagramVersion version = diagramVersionRepository.findById(versionId)
                .orElseThrow(() -> new AppException(ErrorCode.DIAGRAM_VERSION_NOT_FOUND));
        if (!version.getSheet().getId().equals(sheet.getId())) {
            throw new AppException(ErrorCode.DIAGRAM_VERSION_SHEET_MISMATCH);
        }
        return version;
    }

    private DiagramVersion buildVersion(Sheet sheet, User user, DiagramVersionSource source,
                                        String name, String note, String diagramData, String contentHash,
                                        int schemaVersion, DiagramVersion restoredFrom) {
        long nextNumber = diagramVersionRepository.findFirstBySheetOrderByVersionNumberDesc(sheet)
                .map(DiagramVersion::getVersionNumber)
                .orElse(0L) + 1;

        DiagramVersion version = diagramVersionMapper.toDiagramVersion(
                DiagramVersionCreateRequest.builder()
                        .name(name == null || name.isBlank() ? "Version " + nextNumber : name.trim())
                        .note(note == null || note.isBlank() ? null : note.trim())
                        .source(source)
                        .diagramData(diagramData)
                        .build(),
                sheet, user);
        version.setVersionNumber(nextNumber);
        version.setContentHash(contentHash);
        version.setSchemaVersion(schemaVersion);
        version.setRestoredFromVersion(restoredFrom);
        return version;
    }

    /** Giữ tối đa MAX_AUTO bản AUTO / sheet — xóa bản AUTO cũ nhất trước; loại khác giữ nguyên. */
    private void pruneAutoVersions(Sheet sheet) {
        long autoCount = diagramVersionRepository.countBySheetAndSource(sheet, DiagramVersionSource.AUTO);
        if (autoCount <= MAX_AUTO_VERSIONS_PER_SHEET) return;

        List<DiagramVersion> autos = diagramVersionRepository
                .findAllBySheetAndSourceOrderByVersionNumberAsc(sheet, DiagramVersionSource.AUTO);
        List<DiagramVersion> stale = autos.subList(0, (int) (autoCount - MAX_AUTO_VERSIONS_PER_SHEET));
        // Không xóa bản AUTO đang được một bản ghi RESTORE tham chiếu (giữ audit trail nguyên vẹn)
        Set<UUID> referencedIds = diagramVersionRepository.findAllBySheetOrderByVersionNumberDesc(sheet).stream()
                .map(DiagramVersion::getRestoredFromVersion)
                .filter(Objects::nonNull)
                .map(DiagramVersion::getId)
                .collect(Collectors.toSet());
        List<DiagramVersion> deletable = stale.stream()
                .filter(v -> !referencedIds.contains(v.getId()))
                .toList();
        if (!deletable.isEmpty()) {
            diagramVersionRepository.deleteAll(deletable);
            log.info("Pruned {} stale AUTO version(s) for sheet {}", deletable.size(), sheet.getId());
        }
    }

    private JsonNode parseSnapshot(String diagramData) {
        if (diagramData == null || diagramData.isBlank()) {
            throw new AppException(ErrorCode.DIAGRAM_VERSION_DATA_REQUIRED);
        }
        if (diagramData.getBytes(StandardCharsets.UTF_8).length > MAX_SNAPSHOT_BYTES) {
            throw new AppException(ErrorCode.DIAGRAM_VERSION_TOO_LARGE);
        }
        try {
            JsonNode node = objectMapper.readTree(diagramData);
            // Snapshot hợp lệ tối thiểu phải là object có nodes/edges dạng mảng
            if (!node.isObject() || !node.path("nodes").isArray() || !node.path("edges").isArray()) {
                throw new AppException(ErrorCode.DIAGRAM_VERSION_DATA_INVALID);
            }
            return node;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.DIAGRAM_VERSION_DATA_INVALID);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // SHA-256 luôn có trong JRE — nếu tới đây là lỗi môi trường nghiêm trọng
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}