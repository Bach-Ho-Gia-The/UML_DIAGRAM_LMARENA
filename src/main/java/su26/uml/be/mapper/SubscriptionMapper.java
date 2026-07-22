package su26.uml.be.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import su26.uml.be.dto.response.MySubscriptionResponse;
import su26.uml.be.entity.Subscription;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    // status (enum) → String tự map bằng name(); planId/planName lấy từ plan lồng nhau.
    @Mapping(target = "planId", source = "plan.id")
    @Mapping(target = "planName", source = "plan.name")
    MySubscriptionResponse toMySubscriptionResponse(Subscription subscription);
}
