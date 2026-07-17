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
import su26.uml.be.dto.request.DiagramVersionCreateRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.DiagramVersionResponse;
import su26.uml.be.service.adminDashboard.ActivityTrackerService;
import su26.uml.be.service.DiagramVersionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sheets/{sheetId}/versions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Diagram Versions", description = "Diagram version history APIs (checkpoints, restore)")
public class DiagramVersionController {

    DiagramVersionService diagramVersionService;
    ActivityTrackerService activityTracker;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "List versions of a sheet",
            description = "Returns version summaries (newest first) WITHOUT diagramData; fetch a single version for the full snapshot.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Versions returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.DIAGRAM_VERSION_LIST_RESPONSE)))
    public ApiResponse<List<DiagramVersionResponse>> getVersions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId) {
        return diagramVersionService.getVersions(userDetails.getUsername(), sheetId);
    }

    @GetMapping("/{versionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get a version with its full snapshot")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Version returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.DIAGRAM_VERSION_RESPONSE)))
    public ApiResponse<DiagramVersionResponse> getVersion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId,
            @PathVariable UUID versionId) {
        return diagramVersionService.getVersion(userDetails.getUsername(), sheetId, versionId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a version checkpoint",
            description = "Creates a checkpoint of the supplied snapshot. Without force=true, unchanged content (same hash) returns the existing version instead of a new row. AUTO versions are capped per sheet.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Version created (or existing returned when content unchanged).",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.DIAGRAM_VERSION_RESPONSE)))
    public ApiResponse<DiagramVersionResponse> createVersion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId,
            @Valid @RequestBody DiagramVersionCreateRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return diagramVersionService.createVersion(userDetails.getUsername(), sheetId, request);
    }

    @PostMapping("/{versionId}/restore")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Restore a version",
            description = "In one transaction: backs up the current canvas (BEFORE_RESTORE), writes the selected snapshot into the sheet, and appends a RESTORE record referencing the source version. History is never deleted.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Version restored; RESTORE record (with snapshot) returned.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.DIAGRAM_VERSION_RESPONSE)))
    public ApiResponse<DiagramVersionResponse> restoreVersion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId,
            @PathVariable UUID versionId) {
        activityTracker.trackActivity(userDetails.getUsername());
        return diagramVersionService.restoreVersion(userDetails.getUsername(), sheetId, versionId);
    }
}
