package su26.uml.be.mapper;

import org.mapstruct.*;
import su26.uml.be.dto.request.OAuth2UserInfo;
import su26.uml.be.entity.User;

@Mapper(componentModel = "spring")
public interface OAuth2UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "lastActiveAt", ignore = true)
    @Mapping(target = "lastPasswordChangeAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toUser(OAuth2UserInfo info);

    // username/fullName/email/status KHÔNG ignore — OAuth2UserInfo có các field này và flow hiện tại
    // auto-map chúng khi user Google đăng nhập lại (giữ nguyên hành vi).
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "lastActiveAt", ignore = true)
    @Mapping(target = "lastPasswordChangeAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateGoogleFields(OAuth2UserInfo info, @MappingTarget User user);
}
