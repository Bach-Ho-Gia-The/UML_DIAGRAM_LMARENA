package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.dto.request.SheetRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.SheetResponse;
import su26.uml.be.service.adminDashboard.ActivityTrackerService;
import su26.uml.be.service.SheetService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sheets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Sheets", description = "UML Sheet management APIs")
public class SheetController {

    SheetService sheetService;
    ActivityTrackerService activityTracker;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a new sheet", description = "Creates a new drawing sheet for a project.")
    public ApiResponse<SheetResponse> createSheet(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SheetRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return sheetService.createSheet(userDetails.getUsername(), request);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get all sheets for a project", description = "Returns all sheets belonging to a project.")
    public ApiResponse<List<SheetResponse>> getSheetsByProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        return sheetService.getSheetsByProject(userDetails.getUsername(), projectId);
    }

    @GetMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get sheet by ID")
    public ApiResponse<SheetResponse> getSheetById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId) {
        return sheetService.getSheetById(userDetails.getUsername(), sheetId);
    }

    @PatchMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update sheet", description = "Updates sheet name, order, or diagram data.")
    public ApiResponse<SheetResponse> updateSheet(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId,
            @Valid @RequestBody SheetRequest request) {
        activityTracker.trackActivity(userDetails.getUsername());
        return sheetService.updateSheet(userDetails.getUsername(), sheetId, request);
    }

    @DeleteMapping("/{sheetId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete sheet")
    public ApiResponse<Void> deleteSheet(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sheetId) {
        return sheetService.deleteSheet(userDetails.getUsername(), sheetId);
    }
}
