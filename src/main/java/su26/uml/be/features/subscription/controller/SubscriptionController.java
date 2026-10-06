package su26.uml.be.features.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.subscription.dto.DowngradeRequest;
import su26.uml.be.features.subscription.dto.MySubscriptionResponse;
import su26.uml.be.features.subscription.service.SubscriptionService;

/**
 * Hủy / hoàn tác gia hạn + xem subscription hiện tại + hạ cấp có kỳ hạn (booked downgrade).
 * Hủy = graceful downgrade (giữ Premium tới hết kỳ, sau đó về gói base).
 * Hạ cấp trả phí = booked downgrade (pendingPlanId, không thu tiền ngay).
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Subscription", description = "Hủy/hoàn tác gói + xem subscription hiện tại.")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    SubscriptionService subscriptionService;

    @GetMapping("/me/subscription")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Subscription hiện tại", description = "null nếu đang ở gói base (không có sub trả phí).")
    public ApiResponse<MySubscriptionResponse> mySubscription(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("OK", subscriptionService.getMySubscription(userDetails.getUsername()));
    }

    @PostMapping("/subscriptions/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Hủy gia hạn", description = "Giữ Premium tới hết kỳ, sau đó về base. Không thu hồi ngay.")
    public ApiResponse<MySubscriptionResponse> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Đã hủy gia hạn", subscriptionService.cancel(userDetails.getUsername()));
    }

    @PostMapping("/subscriptions/reactivate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Hoàn tác hủy", description = "Bật lại gia hạn khi gói còn hiệu lực.")
    public ApiResponse<MySubscriptionResponse> reactivate(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Đã bật lại gia hạn", subscriptionService.reactivate(userDetails.getUsername()));
    }

    @PostMapping("/subscriptions/downgrade")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Đặt hạ cấp có kỳ hạn (booked downgrade)",
            description = "Ghi pendingPlanId + pendingEffectiveAt (= endDate kỳ hiện tại). KHÔNG thu tiền, "
                    + "KHÔNG đổi quyền ngay — user vẫn dùng gói cũ tới hết kỳ. Hạ về gói mặc định "
                    + "(isDefaultPlan) thì coi như hủy gia hạn (cancelAtPeriodEnd = true). "
                    + "Gói đích phải ACTIVE, bậc thấp hơn gói hiện tại, không phải gói báo giá.")
    public ApiResponse<MySubscriptionResponse> downgrade(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DowngradeRequest request) {
        return ApiResponse.success("Đã đặt hạ cấp",
                subscriptionService.scheduleDowngrade(userDetails.getUsername(), request.getTargetPlanId()));
    }

    @DeleteMapping("/subscriptions/pending-change")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Huỷ thay đổi gói đang chờ",
            description = "Xoá pendingPlanId/pendingEffectiveAt — giữ nguyên gói hiện tại tới hết kỳ.")
    public ApiResponse<MySubscriptionResponse> cancelPendingChange(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Đã huỷ thay đổi đang chờ",
                subscriptionService.cancelPendingChange(userDetails.getUsername()));
    }
}