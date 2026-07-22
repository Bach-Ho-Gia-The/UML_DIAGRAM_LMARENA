package su26.uml.be.features.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import su26.uml.be.features.plan.dto.FeatureCatalogRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.plan.dto.FeatureCatalogResponse;
import su26.uml.be.features.plan.service.FeatureCatalogService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/features")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Feature Catalog", description = "Admin-managed master list of comparable plan features.")
public class FeatureCatalogController {

    FeatureCatalogService featureCatalogService;

    @GetMapping
    @Operation(summary = "List catalog features",
            description = "Returns all catalog features ordered by sortOrder. Used to build the toggle list in the admin plan form and the rows of the pricing comparison matrix.")
    public ApiResponse<List<FeatureCatalogResponse>> getAll() {
        return featureCatalogService.getAll();
    }

    @PostMapping
    @Operation(summary = "Create a catalog feature")
    public ApiResponse<FeatureCatalogResponse> create(@Valid @RequestBody FeatureCatalogRequest request) {
        return featureCatalogService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a catalog feature")
    public ApiResponse<FeatureCatalogResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody FeatureCatalogRequest request) {
        return featureCatalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a catalog feature",
            description = "Removes the feature from the catalog. It automatically disappears from every plan's matrix.")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        return featureCatalogService.delete(id);
    }
}