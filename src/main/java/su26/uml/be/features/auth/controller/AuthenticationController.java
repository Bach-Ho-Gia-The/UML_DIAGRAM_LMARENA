package su26.uml.be.features.auth.controller;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import su26.uml.be.features.auth.dto.IntrospectRequest;
import su26.uml.be.features.auth.dto.LoginRequest;
import su26.uml.be.features.auth.dto.LogoutRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.auth.dto.AuthenticationResponse;
import su26.uml.be.features.auth.dto.IntrospectResponse;
import su26.uml.be.features.auth.service.AuthenticationService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "Login, token introspection, logout and account status checks.")
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/login")
    @SecurityRequirements({})
    @Operation(summary = "Authenticate and obtain a JWT",
            description = "Validates email/password and returns a signed HS512 JWT (valid for 1 hour).")
    ApiResponse<AuthenticationResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.authenticate(request, response))
                .build();
    }

    @PostMapping("/refresh")
    @SecurityRequirements({})
    @Operation(summary = "Refresh access token",
            description = "Reads the REFRESH token from the HttpOnly cookie, rotates it and returns a new ACCESS token.")
    ApiResponse<AuthenticationResponse> refreshToken(HttpServletRequest httpRequest, HttpServletResponse response) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.refreshToken(httpRequest, response))
                .build();
    }

    @PostMapping("/introspect")
    @SecurityRequirements({})
    @Operation(summary = "Introspect (validate) a JWT",
            description = "Returns whether the token is valid, correctly signed, and unexpired.")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        return ApiResponse.<IntrospectResponse>builder()
                .result(authenticationService.introspect(request))
                .build();
    }

    @PostMapping("/logout")
    @SecurityRequirements({})
    @Operation(summary = "Log out (invalidate a JWT)",
            description = "Blacklists the access token and revokes the refresh token cookie.")
    ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) throws ParseException, JOSEException {
        authenticationService.logout(request, httpRequest, response);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/account-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check whether an account is locked",
            description = "Reports lock status by username or email. Always returns 200 to avoid user enumeration.")
    ApiResponse<Map<String, Object>> accountStatus(
            @Parameter(description = "Username or email address to look up.", required = true, example = "johndoe")
            @RequestParam("identifier") String identifier) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("LOCKED", authenticationService.isAccountLocked(identifier));
        return ApiResponse.<Map<String, Object>>builder().result(result).build();
    }
}