package su26.uml.be.features.plan.mapper;

import org.mapstruct.*;
import su26.uml.be.features.plan.dto.FeatureCatalogRequest;
import su26.uml.be.features.plan.dto.FeatureCatalogResponse;
import su26.uml.be.features.plan.entity.FeatureCatalog;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FeatureCatalogMapper {

    FeatureCatalog toEntity(FeatureCatalogRequest request);

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    FeatureCatalogResponse toResponse(FeatureCatalog entity);

    List<FeatureCatalogResponse> toResponseList(List<FeatureCatalog> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(FeatureCatalogRequest request, @MappingTarget FeatureCatalog entity);
}