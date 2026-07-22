package su26.uml.be.features.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.common.config.SwaggerExamples;
import su26.uml.be.features.project.dto.DeleteProjectRequest;
import su26.uml.be.features.project.dto.ProjectRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.dashboard.dto.OwnerGroupResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.project.dto.ProjectResponse;
import su26.uml.be.features.dashboard.dto.ProjectStatsResponse;
import su26.uml.be.features.admin.service.ActivityTrackerService;
import su26.uml.be.features.project.service.ProjectService;

import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Projects", description = "UML Project management APIs")
public class ProjectController {

    ProjectService projectService;
    ActivityTrackerService activityTracker;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a new project", description = "Creates a new UML project and a default sheet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Project created.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.PROJECT_RESPONSE)))
    public ApiResponse<ProjectResponse> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return projectService.createProject(userDetails.getUsername(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get all projects (paginated)",
            description = "Paginated list of projects belonging to the authenticated user. " +
                    "Params: page (0-based), size, sort (e.g. sort=updatedAt,desc), optional isDraft filter. " +
                    "Defaults: page=0, size=20, sort=updatedAt,desc.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Project list returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.PROJECT_LIST_RESPONSE)))
    public ApiResponse<PagedResponse<ProjectResponse>> getAllProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Boolean isDraft,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getAllUserProjects(userDetails.getUsername(), isDraft, pageable);
    }

    @GetMapping("/trash")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get soft-deleted projects in Trash (paginated)",
            description = "Returns soft-deleted projects for the authenticated user.")
    public ApiResponse<PagedResponse<ProjectResponse>> getTrashProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getTrashProjects(userDetails.getUsername(), pageable);
    }

    @GetMapping("/archived")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get archived projects (paginated)",
            description = "Returns active archived projects for the authenticated user.")
    public ApiResponse<PagedResponse<ProjectResponse>> getArchivedProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getArchivedProjects(userDetails.getUsername(), pageable);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all projects for Admin (paginated)",
            description = "Paginated list of all projects in the system. Restricted to ADMIN. " +
                    "Defaults: page=0, size=20, sort=createdAt,desc.")
    public ApiResponse<PagedResponse<ProjectResponse>> getAllProjectsForAdmin(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getAllProjectsForAdmin(pageable);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin project stats",
            description = "Đếm trên toàn bảng (Tổng / Đúng tiến độ / Bản nháp), độc lập với phân trang.")
    public ApiResponse<ProjectStatsResponse> getAdminProjectStats() {
        return projectService.getAdminProjectStats();
    }

    @GetMapping("/admin/owners")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin project owners (paginated, tầng ngoài)",
            description = "Danh sách chủ sở hữu kèm số dự án của mỗi người, sắp xếp theo số dự án giảm dần. " +
                    "Defaults: page=0, size=10.")
    public ApiResponse<PagedResponse<OwnerGroupResponse>> getAdminProjectOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return projectService.getAdminProjectOwners(page, size);
    }

    @GetMapping("/admin/by-owner/{ownerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin projects by owner (paginated, tầng trong)",
            description = "Dự án của một chủ sở hữu, sắp xếp updatedAt desc. Defaults: page=0, size=5.")
    public ApiResponse<PagedResponse<ProjectResponse>> getAdminProjectsByOwner(
            @PathVariable UUID ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return projectService.getAdminProjectsByOwner(ownerId, page, size);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get project by ID", description = "Returns project details if the user owns it.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Project returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.PROJECT_RESPONSE)))
    public ApiResponse<ProjectResponse> getProjectById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        return projectService.getProjectById(projectId, userDetails.getUsername());
    }

    @PatchMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update project", description = "Updates project name and description.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Project updated.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.PROJECT_RESPONSE)))
    public ApiResponse<ProjectResponse> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return projectService.updateProject(projectId, userDetails.getUsername(), request);
    }

    @PatchMapping("/{projectId}/archive")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Toggle project archive status", description = "Toggles isArchived state for a project.")
    public ApiResponse<ProjectResponse> toggleArchiveProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        activityTracker.trackActivity(userDetails.getUsername());
        return projectService.toggleArchiveProject(projectId, userDetails.getUsername());
    }

    @PatchMapping("/{projectId}/restore")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Restore project from Trash", description = "Restores a soft-deleted project (isDeleted=false).")
    public ApiResponse<ProjectResponse> restoreProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        activityTracker.trackActivity(userDetails.getUsername());
        return projectService.restoreProject(projectId, userDetails.getUsername());
    }

    @DeleteMapping("/{projectId}/permanent")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Permanently delete project", description = "Hard-deletes a project from the database permanently.")
    public ApiResponse<Void> permanentDeleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        activityTracker.trackActivity(userDetails.getUsername());
        return projectService.permanentDeleteProject(projectId, userDetails.getUsername());
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete projects", description = "Soft-deletes one or multiple projects and saves version snapshots.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Projects deleted.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.DELETE_PROJECT_RESPONSE)))
    public ApiResponse<Void> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteProjectRequest request) {
        return projectService.deleteProject(request, userDetails.getUsername());
    }
}