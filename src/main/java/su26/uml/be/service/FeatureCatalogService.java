package su26.uml.be.service;

import su26.uml.be.dto.request.FeatureCatalogRequest;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.FeatureCatalogResponse;

import java.util.List;
import java.util.UUID;

public interface FeatureCatalogService {
    ApiResponse<List<FeatureCatalogResponse>> getAll();
    ApiResponse<FeatureCatalogResponse> create(FeatureCatalogRequest request);
    ApiResponse<FeatureCatalogResponse> update(UUID id, FeatureCatalogRequest request);
    ApiResponse<Void> delete(UUID id);
}
