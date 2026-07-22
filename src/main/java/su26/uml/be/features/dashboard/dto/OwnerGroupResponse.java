package su26.uml.be.features.dashboard.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Một nhóm dự án theo chủ sở hữu (tầng ngoài của phân trang admin Projects).
 * Thứ tự field phải khớp constructor expression trong ProjectRepository.findOwnerGroups.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerGroupResponse {
    UUID ownerId;
    String ownerName;
    String ownerEmail;
    Long projectCount;
}