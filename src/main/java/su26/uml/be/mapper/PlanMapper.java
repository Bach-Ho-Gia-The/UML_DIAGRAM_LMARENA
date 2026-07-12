package su26.uml.be.mapper;

import org.mapstruct.*;
import su26.uml.be.dto.request.PlanRequest;
import su26.uml.be.dto.response.PlanResponse;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.PlanFeature;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanMapper {

    // Scalar fields only; planFeatures (limits) and enabledFeatureIds are assembled in the service.
    @Mapping(target = "planFeatures", ignore = true)
    @Mapping(target = "enabledFeatureIds", ignore = true)
    Plan toPlan(PlanRequest request);

    // `features` (matrix cells) is pre-built in the service from the current catalog + this plan's enabled set.
    @Mapping(target = "id", expression = "java(plan.getId() != null ? plan.getId().toString() : null)")
    @Mapping(target = "subscribers", source = "subscribers")
    @Mapping(target = "limits", expression = "java(toLimits(plan.getPlanFeatures()))")
    @Mapping(target = "features", source = "features")
    PlanResponse toPlanResponse(Plan plan, long subscribers, List<PlanResponse.FeatureCell> features);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "planFeatures", ignore = true)
    @Mapping(target = "enabledFeatureIds", ignore = true)
    void updatePlan(PlanRequest request, @MappingTarget Plan plan);

    default PlanResponse.PlanLimits toLimits(List<PlanFeature> features) {
        PlanResponse.PlanLimits.PlanLimitsBuilder builder = PlanResponse.PlanLimits.builder();
        if (features != null) {
            for (PlanFeature f : features) {
                switch (f.getFeatureKey()) {
                    case MAX_PROJECTS -> builder.projects(f.getLimitValue());
                    case MAX_DIAGRAMS -> builder.diagrams(f.getLimitValue());
                    case AI_QUERIES -> builder.aiQueries(f.getLimitValue());
                    case EXPORT_PDF -> builder.exportPdf(f.getLimitValue());
                    case MAX_COLLABORATORS -> builder.collaborators(f.getLimitValue());
                }
            }
        }
        return builder.build();
    }
}
