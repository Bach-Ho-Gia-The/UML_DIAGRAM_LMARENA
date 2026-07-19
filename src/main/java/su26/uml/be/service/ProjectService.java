package su26.uml.be.service;

import org.springframework.data.domain.Pageable;
import su26.uml.be.dto.request.DeleteProjectRequest;
import su26.uml.be.dto.request.ProjectRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.OwnerGroupResponse;
import su26.uml.be.dto.response.PagedResponse;
import su26.uml.be.dto.response.ProjectResponse;
import su26.uml.be.dto.response.ProjectStatsResponse;

import java.util.UUID;

public interface ProjectService {
    ApiResponse<ProjectResponse> createProject(String email, ProjectRequest request);
    ApiResponse<ProjectResponse> updateProject(UUID projectId, String email, ProjectRequest request);
    ApiResponse<Void> deleteProject(DeleteProjectRequest request, String email);
    ApiResponse<ProjectResponse> getProjectById(UUID projectId, String email);
    ApiResponse<PagedResponse<ProjectResponse>> getAllUserProjects(String email, Boolean isDraft, Pageable pageable);
    ApiResponse<PagedResponse<ProjectResponse>> getAllProjectsForAdmin(Pageable pageable);

    // Admin Projects — 3 nguồn dữ liệu độc lập
    ApiResponse<ProjectStatsResponse> getAdminProjectStats();
    ApiResponse<PagedResponse<OwnerGroupResponse>> getAdminProjectOwners(int page, int size);
    ApiResponse<PagedResponse<ProjectResponse>> getAdminProjectsByOwner(UUID ownerId, int page, int size);
}
