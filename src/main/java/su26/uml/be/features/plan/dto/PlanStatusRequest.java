package su26.uml.be.features.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import su26.uml.be.common.constant.enums.PlanStatus;

/**
 * Body của {@code PUT /admin/plans/{id}/status} — đổi trạng thái gói.
 *
 * <p>Status ĐÃ BỎ khỏi {@link PlanRequest}: metadata (tên, giá, limits...) và vòng đời gói
 * (DRAFT → ACTIVE → ARCHIVED) là hai thao tác khác nhau, admin đổi trạng thái qua endpoint này.
 *
 * <p>Chuyển hợp lệ: DRAFT→ACTIVE (bán), DRAFT→ARCHIVED, ACTIVE→ARCHIVED (ngừng bán),
 * ARCHIVED→ACTIVE (bán lại). Chuyển về DRAFT luôn bị từ chối
 * ({@code PLAN_STATE_TRANSITION_DENIED}) — gói đã bán không được quay về nháp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "PlanStatusRequest", description = "Đổi trạng thái gói (admin).")
public class PlanStatusRequest {

    @NotNull(message = "PLAN_STATUS_REQUIRED")
    @Schema(description = "Trạng thái đích: active | archived (draft bị từ chối).", example = "active")
    PlanStatus status;
}
