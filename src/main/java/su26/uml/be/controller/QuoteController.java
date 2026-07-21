package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.subscription.UpgradeQuoteRequest;
import su26.uml.be.dto.subscription.UpgradeQuoteResponse;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.service.UpgradeQuoteService;

/**
 * Báo giá nâng cấp gói (read-only). Sau feature flag {@code feature.quote-v2-enabled}:
 * OFF (mặc định) → {@link ErrorCode#FEATURE_DISABLED} (404), giữ baseline không đổi (Chặng 1C).
 */
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Subscription", description = "Nâng cấp gói (quote/proration).")
@SecurityRequirement(name = "bearerAuth")
public class QuoteController {

    UpgradeQuoteService upgradeQuoteService;

    @Value("${feature.quote-v2-enabled:false}")
    @NonFinal
    boolean quoteV2Enabled;

    @PostMapping("/upgrade/quote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Quote nâng cấp gói", description = "Tính tiền prorated + quota delta khi nâng lên gói bậc cao hơn. Read-only.")
    public ApiResponse<UpgradeQuoteResponse> quote(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpgradeQuoteRequest request) {
        if (!quoteV2Enabled) {
            throw new AppException(ErrorCode.FEATURE_DISABLED);
        }
        return ApiResponse.success("OK",
                upgradeQuoteService.getQuote(userDetails.getUsername(), request.getTargetPlanId()));
    }
}
