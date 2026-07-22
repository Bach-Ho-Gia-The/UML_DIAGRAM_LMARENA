package su26.uml.be.mapper;

import org.mapstruct.*;
import su26.uml.be.dto.request.SheetRequest;
import su26.uml.be.dto.response.SheetResponse;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.Sheet;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SheetMapper {
    Sheet toSheet(SheetRequest request);

    // Tạo Sheet từ các thành phần rời (diagram item mới, bản sao diagram, sheet mặc định của project).
    // Các field trùng tên param (name/orderIndex/diagramData/diagramType/project) MapStruct tự map;
    // ignore id/createdAt/updatedAt để không bị auto-map nhầm từ project (project.id → sheet.id).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // FIX: Sheet + Project cùng có lifecycleStatus/archivedAt/purgeAt (thêm ở commit 8153c63).
    // MapStruct 1.5.5 ưu tiên nested-map (project.lifecycleStatus → sheet.lifecycleStatus) và BỎ QUÊN
    // gán sheet.project = project → project_id null → NOT NULL violation → tạo project/diagram gãy.
    // Ép gán project trực tiếp + ignore 3 field lifecycle (sheet mới mặc định ACTIVE theo @Builder.Default).
    @Mapping(target = "project", source = "project")
    @Mapping(target = "lifecycleStatus", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    @Mapping(target = "purgeAt", ignore = true)
    Sheet toSheet(String name, Integer orderIndex, String diagramData, String diagramType, Project project);

    @Mapping(target = "projectId", source = "project.id")
    SheetResponse toSheetResponse(Sheet sheet);

    List<SheetResponse> toSheetResponseList(List<Sheet> sheets);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSheet(SheetRequest request, @MappingTarget Sheet sheet);
}
