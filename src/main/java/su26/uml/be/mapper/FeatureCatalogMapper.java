package su26.uml.be.mapper;

import org.mapstruct.*;
import su26.uml.be.dto.request.FeatureCatalogRequest;
import su26.uml.be.dto.response.FeatureCatalogResponse;
import su26.uml.be.entity.FeatureCatalog;

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
