package su26.uml.be.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import su26.uml.be.dto.request.CompleteProfileRequest;
import su26.uml.be.dto.request.UpdateUserRequest;
import su26.uml.be.dto.request.UserRegisterRequest;
import su26.uml.be.dto.response.DeleteAccountResponse;
import su26.uml.be.dto.response.MeResponse;
import su26.uml.be.dto.response.UserResponse;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "username", source = "email")
    User toUser(UserRegisterRequest request);

    @Mapping(target = "role", source = "role")
    @Mapping(target = "createdAt", source = "createdAt")
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    @Mapping(target = "role", source = "role.roleName")
    @Mapping(target = "currentPlanId", source = "currentSubscription.plan.id")
    // profileCompleted maps directly from the User.profileCompleted column (source of truth).
    MeResponse toMeResponse(User user);

    // Gói hiệu lực → tag trên MeResponse. ignoreByDefault để KHÔNG map nhầm plan.id → MeResponse.id (user id).
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "effectivePlanId", source = "id")
    @Mapping(target = "planName", source = "name")
    @Mapping(target = "planColor", source = "color")
    void applyEffectivePlan(Plan plan, @MappingTarget MeResponse target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(UpdateUserRequest request, @MappingTarget User user);

    // Onboarding: only personal info is mapped here; the password hash is set in the service.
    @Mapping(target = "password", ignore = true)
    void completeProfile(CompleteProfileRequest request, @MappingTarget User user);

    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "deletionDate", source = "user.deletionDate")
    @Mapping(target = "daysRemaining", source = "daysRemaining")
    @Mapping(target = "message", source = "message")
    DeleteAccountResponse toDeleteAccountResponse(User user, Long daysRemaining, String message);
}
