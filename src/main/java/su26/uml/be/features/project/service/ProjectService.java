package su26.uml.be.features.project.service;

import org.springframework.data.domain.Pageable;
import su26.uml.be.features.project.dto.DeleteProjectRequest;
import su26.uml.be.features.project.dto.ProjectRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.dashboard.dto.OwnerGroupResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.project.dto.ProjectResponse;
import su26.uml.be.features.dashboard.dto.ProjectStatsResponse;

import java.util.UUID;

public interface ProjectService {
    ApiResponse<ProjectResponse> createProject(String email, ProjectRequest request);
    ApiResponse<ProjectResponse> updateProject(UUID projectId, String email, ProjectRequest request);
    ApiResponse<Void> deleteProject(DeleteProjectRequest request, String email);
    ApiResponse<ProjectResponse> getProjectById(UUID projectId, String email);
    ApiResponse<PagedResponse<ProjectResponse>> getAllUserProjects(String email, Boolean isDraft, Pageable pageable);
    ApiResponse<PagedResponse<ProjectResponse>> getTrashProjects(String email, Pageable pageable);
    ApiResponse<PagedResponse<ProjectResponse>> getArchivedProjects(String email, Pageable pageable);
    ApiResponse<ProjectResponse> toggleArchiveProject(UUID projectId, String email);
    ApiResponse<ProjectResponse> restoreProject(UUID projectId, String email);
    ApiResponse<Void> permanentDeleteProject(UUID projectId, String email);
    ApiResponse<PagedResponse<ProjectResponse>> getAllProjectsForAdmin(Pageable pageable);

    // Admin Projects — 3 nguồn dữ liệu độc lập
    ApiResponse<ProjectStatsResponse> getAdminProjectStats();
    ApiResponse<PagedResponse<OwnerGroupResponse>> getAdminProjectOwners(int page, int size);
    ApiResponse<PagedResponse<ProjectResponse>> getAdminProjectsByOwner(UUID ownerId, int page, int size);
}