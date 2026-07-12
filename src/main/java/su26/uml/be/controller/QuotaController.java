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
import org.springframework.web.bind.annotation.RestController;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.QuotaResponse;
import su26.uml.be.service.QuotaService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Quota", description = "Hạn mức AI Request theo kỳ của user.")
@SecurityRequirement(name = "bearerAuth")
public class QuotaController {

    QuotaService quotaService;

    @GetMapping("/me/quota")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get my AI quota", description = "Số AI request đã dùng / hạn mức + ngày reset (đã áp lazy reset).")
    public ApiResponse<QuotaResponse> myQuota(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("OK", quotaService.getMyQuota(userDetails.getUsername()));
    }
}
