package su26.uml.be.features.plan.service;

import su26.uml.be.features.plan.dto.FeatureCatalogRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.plan.dto.FeatureCatalogResponse;

import java.util.List;
import java.util.UUID;

public interface FeatureCatalogService {
    ApiResponse<List<FeatureCatalogResponse>> getAll();
    ApiResponse<FeatureCatalogResponse> create(FeatureCatalogRequest request);
    ApiResponse<FeatureCatalogResponse> update(UUID id, FeatureCatalogRequest request);
    ApiResponse<Void> delete(UUID id);
}