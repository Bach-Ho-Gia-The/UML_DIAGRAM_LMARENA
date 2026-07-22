package su26.uml.be.features.workspace.mapper;

import org.mapstruct.*;
import su26.uml.be.features.workspace.dto.WorkspaceItemCreateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemUpdateRequest;
import su26.uml.be.features.workspace.dto.WorkspaceItemResponse;
import su26.uml.be.features.project.entity.Project;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.workspace.entity.WorkspaceItem;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkspaceItemMapper {

    // project/parent/sheet/orderIndex/createdBy do service gán (cần lookup entity)
    @Mapping(target = "markdownContent", source = "content")
    WorkspaceItem toWorkspaceItem(WorkspaceItemCreateRequest request);

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "sheetId", source = "sheet.id")
    @Mapping(target = "diagramType", source = "sheet.diagramType")
    @Mapping(target = "content", source = "markdownContent")
    WorkspaceItemResponse toWorkspaceItemResponse(WorkspaceItem item);

    List<WorkspaceItemResponse> toWorkspaceItemResponseList(List<WorkspaceItem> items);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "markdownContent", source = "content")
    void updateWorkspaceItem(WorkspaceItemUpdateRequest request, @MappingTarget WorkspaceItem item);

    // Item DIAGRAM gốc cho một sheet (compat hook API sheet cũ, sheet mặc định, backfill).
    // sheet/createdBy trùng tên param → auto-map; name/orderIndex/project phải chỉ rõ source
    // vì trùng ứng viên với sheet.name/sheet.orderIndex/sheet.project (ambiguous nếu bỏ).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "kind", constant = "DIAGRAM")
    @Mapping(target = "orderIndex", source = "orderIndex")
    @Mapping(target = "project", source = "project")
    WorkspaceItem toDiagramItem(Sheet sheet, String name, Integer orderIndex, Project project, User createdBy);

    // Bản sao item cho duplicate — sheet/orderIndex service gán sau. KHÔNG bỏ dòng nào ở đây:
    // source và parent cùng là WorkspaceItem nên mọi field đều có ≥2 ứng viên (source.X / parent.X)
    // → thiếu @Mapping là lỗi compile ambiguous hoặc auto-map sai (vd copy id/version của source).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "kind", source = "source.kind")
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "markdownContent", source = "source.markdownContent")
    @Mapping(target = "project", source = "project")
    @Mapping(target = "parent", source = "parent")
    @Mapping(target = "sheet", ignore = true)
    @Mapping(target = "createdBy", source = "createdBy")
    WorkspaceItem copyItem(WorkspaceItem source, String name, WorkspaceItem parent, Project project, User createdBy);
}