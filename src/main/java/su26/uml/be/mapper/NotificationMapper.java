package su26.uml.be.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import su26.uml.be.dto.response.NotificationResponse;
import su26.uml.be.entity.Notification;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    // Entity field `boolean isRead` → getter isRead() → property "read"; target DTO property là "isRead"
    // nên MapStruct không tự khớp — thiếu dòng này thì response luôn trả isRead=false.
    @Mapping(target = "isRead", source = "read")
    @Mapping(target = "severity", source = "severity")
    NotificationResponse toNotificationResponse(Notification notification);

    List<NotificationResponse> toNotificationResponseList(List<Notification> notifications);
}
