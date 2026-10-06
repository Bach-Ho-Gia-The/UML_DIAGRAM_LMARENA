package su26.uml.be.features.subscription.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import su26.uml.be.features.subscription.dto.MySubscriptionResponse;
import su26.uml.be.features.subscription.entity.Subscription;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    // status (enum) → String tự map bằng name(); planId/planName lấy từ plan lồng nhau.
    // pendingPlanId / pendingEffectiveAt map tự động theo tên field.
    // pendingPlanName / pendingPrice cần tra PlanRepository → service set sau (mapper chỉ có Subscription).
    @Mapping(target = "planId", source = "plan.id")
    @Mapping(target = "planName", source = "plan.name")
    @Mapping(target = "pendingPlanName", ignore = true)
    @Mapping(target = "pendingPrice", ignore = true)
    MySubscriptionResponse toMySubscriptionResponse(Subscription subscription);
}