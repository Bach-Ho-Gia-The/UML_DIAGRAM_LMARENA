package su26.uml.be.mapper;

import org.mapstruct.*;
import su26.uml.be.dto.request.ProjectRequest;
import su26.uml.be.dto.response.ProjectResponse;
import su26.uml.be.entity.Project;

import java.util.List;

@Mapper(componentModel = "spring", uses = {SheetMapper.class})
public interface ProjectMapper {
    Project toProject(ProjectRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "ownerName", source = "user.fullName")
    @Mapping(target = "ownerEmail", source = "user.email")
    // Entity field `boolean isDraft` → getter isDraft() → property "draft"; target DTO (Boolean) property là
    // "isDraft" nên MapStruct không tự khớp — thiếu dòng này thì response luôn trả isDraft=null.
    @Mapping(target = "isDraft", source = "draft")
    @Mapping(target = "isArchived", source = "archived")
    @Mapping(target = "diagramCount", expression = "java(project.getSheets() != null ? project.getSheets().size() : 0)")
    ProjectResponse toProjectResponse(Project project);

    List<ProjectResponse> toProjectResponseList(List<Project> projects);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // Chiều update dùng setter: entity setDraft() → target property "draft", còn request (Boolean) là
    // "isDraft" — thiếu dòng này thì PATCH isDraft bị bỏ qua âm thầm. IGNORE null giữ semantics partial update.
    @Mapping(target = "draft", source = "isDraft")
    void updateProject(ProjectRequest request, @MappingTarget Project project);
}
