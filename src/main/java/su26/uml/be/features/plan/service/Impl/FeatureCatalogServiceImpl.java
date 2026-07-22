package su26.uml.be.features.plan.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import su26.uml.be.features.plan.dto.FeatureCatalogRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.plan.dto.FeatureCatalogResponse;
import su26.uml.be.features.plan.entity.FeatureCatalog;
import su26.uml.be.common.exception.AppException;
import su26.uml.be.common.exception.ErrorCode;
import su26.uml.be.features.plan.mapper.FeatureCatalogMapper;
import su26.uml.be.features.plan.repository.FeatureCatalogRepository;
import su26.uml.be.features.plan.service.FeatureCatalogService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FeatureCatalogServiceImpl implements FeatureCatalogService {

    FeatureCatalogRepository featureCatalogRepository;
    FeatureCatalogMapper featureCatalogMapper;

    @Override
    public ApiResponse<List<FeatureCatalogResponse>> getAll() {
        List<FeatureCatalogResponse> result = featureCatalogMapper.toResponseList(
                featureCatalogRepository.findAllByOrderBySortOrderAscLabelAsc());
        return ApiResponse.success("OK", result);
    }

    @Override
    public ApiResponse<FeatureCatalogResponse> create(FeatureCatalogRequest request) {
        if (featureCatalogRepository.existsByLabelIgnoreCase(request.getLabel())) {
            throw new AppException(ErrorCode.FEATURE_LABEL_EXISTED);
        }
        FeatureCatalog entity = featureCatalogMapper.toEntity(request);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        FeatureCatalog saved = featureCatalogRepository.save(entity);
        return ApiResponse.success("Tạo tính năng thành công", featureCatalogMapper.toResponse(saved));
    }

    @Override
    public ApiResponse<FeatureCatalogResponse> update(UUID id, FeatureCatalogRequest request) {
        FeatureCatalog entity = featureCatalogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEATURE_NOT_FOUND));

        if (request.getLabel() != null
                && !request.getLabel().equalsIgnoreCase(entity.getLabel())
                && featureCatalogRepository.existsByLabelIgnoreCase(request.getLabel())) {
            throw new AppException(ErrorCode.FEATURE_LABEL_EXISTED);
        }

        featureCatalogMapper.updateEntity(request, entity);
        FeatureCatalog saved = featureCatalogRepository.save(entity);
        return ApiResponse.success("Cập nhật tính năng thành công", featureCatalogMapper.toResponse(saved));
    }

    @Override
    public ApiResponse<Void> delete(UUID id) {
        FeatureCatalog entity = featureCatalogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEATURE_NOT_FOUND));
        // Stale ids left in Plan.enabledFeatureIds are harmless: the matrix is rebuilt from the
        // current catalog, so a deleted feature simply disappears from every plan.
        featureCatalogRepository.delete(entity);
        return ApiResponse.<Void>builder().build();
    }
}