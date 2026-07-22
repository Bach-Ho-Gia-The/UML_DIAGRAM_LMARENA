package su26.uml.be.features.project.service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import su26.uml.be.features.project.dto.DeleteProjectRequest;
import su26.uml.be.features.project.dto.ProjectRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.dashboard.dto.OwnerGroupResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.project.dto.ProjectResponse;
import su26.uml.be.features.dashboard.dto.ProjectStatsResponse;
import su26.uml.be.features.project.entity.Project;
import su26.uml.be.features.project.entity.ProjectVersion;
import su26.uml.be.features.project.entity.Sheet;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.PlanFeatureKey;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.project.mapper.ProjectMapper;
import su26.uml.be.features.project.mapper.SheetMapper;
import su26.uml.be.features.workspace.mapper.WorkspaceItemMapper;
import su26.uml.be.features.project.repository.ProjectRepository;
import su26.uml.be.features.project.repository.ProjectVersionRepository;
import su26.uml.be.features.project.repository.SheetRepository;
import su26.uml.be.features.workspace.repository.WorkspaceItemRepository;
import su26.uml.be.features.diagram.repository.DiagramVersionRepository;
import su26.uml.be.features.plan.service.PlanLimitService;
import su26.uml.be.features.project.service.ProjectService;
import su26.uml.be.infrastructure.socket.SocketService;
import su26.uml.be.features.user.repository.UserRepository;

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
    WorkspaceItemRepository workspaceItemRepository;
    ProjectMapper projectMapper;
    SheetMapper sheetMapper;
    WorkspaceItemMapper workspaceItemMapper;
    SocketService socketService;
    PlanLimitService planLimitService;
    DiagramVersionRepository diagramVersionRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiResponse<ProjectResponse> createProject(String email, ProjectRequest request) {
        if (request.getProjectName() == null || request.getProjectName().isBlank()) {
            throw new AppException(ErrorCode.PROJECT_NAME_REQUIRED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        planLimitService.assertCanCreate(user.getId(), PlanFeatureKey.MAX_PROJECTS,
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
        Sheet defaultSheet = sheetMapper.toSheet(
                "Sheet 1", 0, "{\"nodes\": [], \"edges\": []}", "activity", savedProject);
        sheetRepository.save(defaultSheet);

        // Thêm vào list để response có dữ liệu sheet ngay lập tức
        savedProject.getSheets().add(defaultSheet);

        // Sheet mặc định cũng phải hiện trong cây workspace: tạo DIAGRAM item gốc cùng tx
        workspaceItemRepository.save(
                workspaceItemMapper.toDiagramItem(defaultSheet, defaultSheet.getName(), 0, savedProject, user));

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
    public ApiResponse<PagedResponse<ProjectResponse>> getAllUserProjects(String email, Boolean isDraft, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Page<Project> projects;
        if (isDraft == null) {
            projects = projectRepository.findAllByUserAndIsDeletedFalseAndIsArchivedFalse(user, pageable);
        } else if (isDraft) {
            projects = projectRepository.findAllByUserAndIsDeletedFalseAndIsArchivedFalseAndIsDraftTrue(user, pageable);
        } else {
            projects = projectRepository.findAllByUserAndIsDeletedFalseAndIsArchivedFalseAndIsDraftFalse(user, pageable);
        }
        return ApiResponse.success("Lấy danh sách dự án thành công",
                PagedResponse.from(projects.map(projectMapper::toProjectResponse)));
    }

    @Override
    public ApiResponse<PagedResponse<ProjectResponse>> getTrashProjects(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Page<Project> projects = projectRepository.findAllByUserAndIsDeletedTrue(user, pageable);
        return ApiResponse.success("Lấy danh sách thùng rác thành công",
                PagedResponse.from(projects.map(projectMapper::toProjectResponse)));
    }

    @Override
    public ApiResponse<PagedResponse<ProjectResponse>> getArchivedProjects(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Page<Project> projects = projectRepository.findAllByUserAndIsDeletedFalseAndIsArchivedTrue(user, pageable);
        return ApiResponse.success("Lấy danh sách lưu trữ thành công",
                PagedResponse.from(projects.map(projectMapper::toProjectResponse)));
    }

    @Override
    public ApiResponse<ProjectResponse> toggleArchiveProject(UUID projectId, String email) {
        Project project = getProjectAndValidateOwnership(projectId, email);
        project.setArchived(!project.isArchived());
        project.setUpdatedAt(LocalDateTime.now());
        Project saved = projectRepository.save(project);
        String msg = saved.isArchived() ? "Đã lưu trữ dự án" : "Đã hủy lưu trữ dự án";
        return ApiResponse.success(msg, projectMapper.toProjectResponse(saved));
    }

    @Override
    public ApiResponse<ProjectResponse> restoreProject(UUID projectId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        if (!project.getUser().getId().equals(user.getId()) && !user.getRole().getRoleName().equals("ADMIN")) {
            throw new AppException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        project.setDeleted(false);
        project.setUpdatedAt(LocalDateTime.now());
        Project saved = projectRepository.save(project);
        return ApiResponse.success("Khôi phục dự án thành công", projectMapper.toProjectResponse(saved));
    }

    @Override
    @Transactional
    public ApiResponse<Void> permanentDeleteProject(UUID projectId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        if (!project.getUser().getId().equals(user.getId()) && !user.getRole().getRoleName().equals("ADMIN")) {
            throw new AppException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        // Dùng phương thức chuẩn của Spring Data JPA (Derived Query Methods) — KHÔNG DÙNG @Query
        List<Sheet> sheets = sheetRepository.findAllByProjectOrderByOrderIndexAsc(project);
        if (!sheets.isEmpty()) {
            diagramVersionRepository.deleteAllBySheetIn(sheets);
        }
        workspaceItemRepository.deleteAllByProject(project);
        projectVersionRepository.deleteAllByProject(project);
        sheetRepository.deleteAllByProject(project);
        projectRepository.delete(project);

        return ApiResponse.success("Xóa vĩnh viễn dự án thành công");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<ProjectResponse>> getAllProjectsForAdmin(Pageable pageable) {
        Page<Project> projects = projectRepository.findAllByIsDeletedFalseAndIsDraftFalse(pageable);
        return ApiResponse.success("Lấy danh sách tất cả dự án thành công (Admin)",
                PagedResponse.from(projects.map(projectMapper::toProjectResponse)));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ProjectStatsResponse> getAdminProjectStats() {
        long total = projectRepository.countByIsDeletedFalse();
        long drafts = projectRepository.countByIsDeletedFalseAndIsDraftTrue();
        long onTrack = projectRepository.countByIsDeletedFalseAndIsDraftFalse();
        ProjectStatsResponse stats = ProjectStatsResponse.builder()
                .total(total)
                .onTrack(onTrack)
                .needAttention(0)
                .drafts(drafts)
                .build();
        return ApiResponse.success("Lấy thống kê dự án thành công (Admin)", stats);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<OwnerGroupResponse>> getAdminProjectOwners(int page, int size) {
        // ORDER BY đã cố định trong query (count desc) → PageRequest không kèm Sort
        Page<OwnerGroupResponse> owners = projectRepository.findOwnerGroups(PageRequest.of(page, size));
        return ApiResponse.success("Lấy danh sách chủ sở hữu dự án thành công (Admin)",
                PagedResponse.from(owners));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<ProjectResponse>> getAdminProjectsByOwner(UUID ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Project> projects = projectRepository.findAllByUser_IdAndIsDeletedFalse(ownerId, pageable);
        return ApiResponse.success("Lấy danh sách dự án theo chủ sở hữu thành công (Admin)",
                PagedResponse.from(projects.map(projectMapper::toProjectResponse)));
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