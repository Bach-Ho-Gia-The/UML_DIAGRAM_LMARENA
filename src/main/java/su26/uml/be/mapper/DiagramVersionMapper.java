package su26.uml.be.mapper;

import org.mapstruct.*;
import su26.uml.be.dto.request.DiagramVersionCreateRequest;
import su26.uml.be.dto.response.DiagramVersionResponse;
import su26.uml.be.entity.DiagramVersion;
import su26.uml.be.entity.Sheet;
import su26.uml.be.entity.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DiagramVersionMapper {

    @Mapping(target = "sheetId", source = "sheet.id")
    @Mapping(target = "restoredFromVersionId", source = "restoredFromVersion.id")
    DiagramVersionResponse toDiagramVersionResponse(DiagramVersion version);

    // Bản tóm tắt cho danh sách: bỏ diagramData (payload lớn, client lazy-load khi chọn).
    // @Named + qualifiedByName để hết ambiguous với toDiagramVersionResponse (cùng chữ ký).
    @Named("toDiagramVersionSummary")
    @Mapping(target = "sheetId", source = "sheet.id")
    @Mapping(target = "restoredFromVersionId", source = "restoredFromVersion.id")
    @Mapping(target = "diagramData", ignore = true)
    DiagramVersionResponse toDiagramVersionSummary(DiagramVersion version);

    @IterableMapping(qualifiedByName = "toDiagramVersionSummary")
    List<DiagramVersionResponse> toDiagramVersionSummaryList(List<DiagramVersion> versions);

    // versionNumber/contentHash/schemaVersion/restoredFromVersion do service tính rồi gán;
    // ignore id/createdAt/updatedAt (DB sinh). name/diagramData phải chỉ rõ source = request.*
    // vì sheet cũng có property trùng tên (multi-source ambiguous nếu bỏ).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "schemaVersion", ignore = true)
    @Mapping(target = "restoredFromVersion", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "note", source = "request.note")
    @Mapping(target = "source", source = "request.source")
    @Mapping(target = "diagramData", source = "request.diagramData")
    @Mapping(target = "sheet", source = "sheet")
    @Mapping(target = "createdBy", source = "createdBy")
    DiagramVersion toDiagramVersion(DiagramVersionCreateRequest request, Sheet sheet, User createdBy);
}
