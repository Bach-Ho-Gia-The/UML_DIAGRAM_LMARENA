package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.config.swagger.SwaggerExamples;
import su26.uml.be.dto.request.DeleteProjectRequest;
import su26.uml.be.dto.request.ProjectRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.ProjectResponse;
import su26.uml.be.service.adminDashboard.ActivityTrackerService;
import su26.uml.be.service.ProjectService;

import java.util.List;
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
    @Operation(summary = "Get all projects", description = "Returns all projects belonging to the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Project list returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.PROJECT_LIST_RESPONSE)))
    public ApiResponse<List<ProjectResponse>> getAllProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Boolean isDraft) {
        return projectService.getAllUserProjects(userDetails.getUsername(), isDraft);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all projects for Admin", description = "Returns all projects in the system. Restricted to ADMIN.")
    public ApiResponse<List<ProjectResponse>> getAllProjectsForAdmin() {
        return projectService.getAllProjectsForAdmin();
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
