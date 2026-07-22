package su26.uml.be.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.MySubscriptionResponse;
import su26.uml.be.service.SubscriptionService;

/**
 * Hủy / hoàn tác gia hạn + xem subscription hiện tại. Hủy = graceful downgrade
 * (giữ Premium tới hết kỳ, sau đó về gói base).
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
}
