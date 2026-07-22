package su26.uml.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.subscription.QuotePairResponse;
import su26.uml.be.dto.subscription.UpgradeQuoteRequest;
import su26.uml.be.service.UpgradeQuoteService;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Subscription", description = "Nâng cấp gói (quote/proration).")
@SecurityRequirement(name = "bearerAuth")
public class QuoteController {

    UpgradeQuoteService upgradeQuoteService;

    @PostMapping("/upgrade/quote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Quote nâng cấp gói", description = "Trả về cả 2 hình thức: tiết kiệm (prorated) và thẳng (direct). Read-only.")
    public ApiResponse<QuotePairResponse> quote(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpgradeQuoteRequest request) {
        return ApiResponse.success("OK",
                upgradeQuoteService.getQuotePair(userDetails.getUsername(), request.getTargetPlanId()));
    }
}
