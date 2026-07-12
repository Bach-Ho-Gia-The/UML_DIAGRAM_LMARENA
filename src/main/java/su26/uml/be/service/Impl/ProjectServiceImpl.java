package su26.uml.be.service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.dto.request.DeleteProjectRequest;
import su26.uml.be.dto.request.ProjectRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.ProjectResponse;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.ProjectVersion;
import su26.uml.be.entity.Sheet;
import su26.uml.be.entity.User;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.mapper.ProjectMapper;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.repository.ProjectVersionRepository;
import su26.uml.be.repository.SheetRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.ProjectService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class ProjectServiceImpl implements ProjectService {
    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectVersionRepository projectVersionRepository;
    SheetRepository sheetRepository;
    ProjectMapper projectMapper;
    su26.uml.be.service.SocketService socketService;
    su26.uml.be.service.PlanLimitService planLimitService;
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiResponse<ProjectResponse> createProject(String email, ProjectRequest request) {
        if (request.getProjectName() == null || request.getProjectName().isBlank()) {
            throw new AppException(ErrorCode.PROJECT_NAME_REQUIRED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        planLimitService.assertCanCreate(user.getId(), su26.uml.be.enums.PlanFeatureKey.MAX_PROJECTS,
                projectRepository.countByUserAndIsDeletedFalse(user));

        Project project = projectMapper.toProject(request);
        project.setUser(user);
        if (request.getIsDraft() != null) {
            project.setDraft(request.getIsDraft());
        }
        if (request.getPublicAccess() != null) {
            project.setPublicAccess(request.getPublicAccess());
        }
        
        Project savedProject = projectRepository.save(project);

        // Tạo Sheet mặc định cho Canvas JSON
        Sheet defaultSheet = Sheet.builder()
                .name("Sheet 1")
                .orderIndex(0)
                .diagramData("{\"nodes\": [], \"edges\": []}")
                .project(savedProject)
                .build();
        sheetRepository.save(defaultSheet);
        
        // Thêm vào list để response có dữ liệu sheet ngay lập tức
        savedProject.getSheets().add(defaultSheet);

        log.info("Project created: {} with default sheet for user: {}", savedProject.getId(), email);
        return ApiResponse.success("Tạo dự án thành công", projectMapper.toProjectResponse(savedProject));
    }

    @Override
    public ApiResponse<ProjectResponse> updateProject(UUID projectId, String email, ProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.isDeleted()) {
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isAdmin = user.getRole().getRoleName().equals("ADMIN");
        boolean isOwner = project.getUser().getEmail().equals(email);

        // ONLY Owner or Admin can update project
        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        projectMapper.updateProject(request, project);
        if (request.getIsDraft() != null) {
            project.setDraft(request.getIsDraft());
        }
        if (request.getPublicAccess() != null) {
            boolean wasPublic = Boolean.TRUE.equals(project.getPublicAccess());
            boolean isNowPublic = Boolean.TRUE.equals(request.getPublicAccess());
            
            project.setPublicAccess(request.getPublicAccess());
            
            // If changed from Public to Private, kick everyone out
            if (wasPublic && !isNowPublic) {
                socketService.broadcastCollabDisabled(projectId);
            }
        }
        Project updatedProject = projectRepository.save(project);

        return ApiResponse.success("Cập nhật dự án thành công", projectMapper.toProjectResponse(updatedProject));
    }

    @Override
    public ApiResponse<Void> deleteProject(DeleteProjectRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        boolean isAdmin = user.getRole().getRoleName().equals("ADMIN");

        List<Project> projects = projectRepository.findAllByIdIn(request.getIds());
        
        for (Project project : projects) {
            if (!isAdmin && !project.getUser().getEmail().equals(email)) {
                throw new AppException(ErrorCode.PROJECT_ACCESS_DENIED);
            }
            
            saveProjectVersion(project);
            
            project.setDeleted(true);
            project.setUpdatedAt(LocalDateTime.now());
        }
        
        projectRepository.saveAll(projects);
        
        log.info("Bulk soft-deleted {} projects by user/admin: {}", projects.size(), email);
        return ApiResponse.success("Xóa các dự án thành công");
    }

    @Override
    public ApiResponse<ProjectResponse> getProjectById(UUID projectId, String email) {
        Project project = getProjectAndValidateOwnership(projectId, email);
        return ApiResponse.success("Lấy thông tin dự án thành công", projectMapper.toProjectResponse(project));
    }

    @Override
    public ApiResponse<List<ProjectResponse>> getAllUserProjects(String email, Boolean isDraft) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Project> projects;
        if (isDraft == null) {
            projects = projectRepository.findAllByUserAndIsDeletedFalse(user);
        } else if (isDraft) {
            projects = projectRepository.findAllByUserAndIsDeletedFalseAndIsDraftTrue(user);
        } else {
            projects = projectRepository.findAllByUserAndIsDeletedFalseAndIsDraftFalse(user);
        }
        return ApiResponse.success("Lấy danh sách dự án thành công", projectMapper.toProjectResponseList(projects));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProjectResponse>> getAllProjectsForAdmin() {
        List<Project> projects = projectRepository.findAllByIsDeletedFalseAndIsDraftFalse();
        return ApiResponse.success("Lấy danh sách tất cả dự án thành công (Admin)", projectMapper.toProjectResponseList(projects));
    }

    private Project getProjectAndValidateOwnership(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.isDeleted()) {
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isAdmin = user.getRole().getRoleName().equals("ADMIN");

        // Allow access if user is owner OR user is admin OR project is public
        if (!isAdmin && !project.getUser().getEmail().equals(email) && !Boolean.TRUE.equals(project.getPublicAccess())) {
            throw new AppException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        return project;
    }

    private void saveProjectVersion(Project project) {
        Integer lastVersion = projectVersionRepository.findFirstByProjectOrderByVersionNumberDesc(project)
                .map(ProjectVersion::getVersionNumber)
                .orElse(0);

        String snapshot = "";
        try {
            // Lưu snapshot là danh sách các sheet dưới dạng JSON
            List<Sheet> sheets = sheetRepository.findAllByProjectOrderByOrderIndexAsc(project);
            snapshot = objectMapper.writeValueAsString(sheets.stream()
                    .map(s -> java.util.Map.of(
                            "name", s.getName(),
                            "data", s.getDiagramData() != null ? s.getDiagramData() : ""
                    ))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to create project snapshot", e);
        }

        ProjectVersion version = ProjectVersion.builder()
                .project(project)
                .projectSnapshot(snapshot)
                .versionNumber(lastVersion + 1)
                .build();
        
        projectVersionRepository.save(version);
    }
}
