package su26.uml.be.features.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.features.ai.dto.*;
import su26.uml.be.features.user.dto.*;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.user.dto.DeleteAccountResponse;
import su26.uml.be.features.user.dto.MeResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.user.dto.UserResponse;
import su26.uml.be.features.admin.dto.AdminSetPasswordRequest;
import su26.uml.be.features.admin.dto.AdminUpdateUserRequest;
import su26.uml.be.features.user.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Users", description = "User registration and account management.")
public class AccountController {

    UserService userService;

    // ─── Public ─────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @SecurityRequirements({})
    @Operation(summary = "Register a new user account",
            description = "Creates a new user. Username defaults to email; role defaults to USER.")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return userService.registerUser(request);
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements({})
    @Operation(summary = "Request a password-reset OTP",
            description = "Generates a 6-digit OTP (valid 90 s) and emails it. Returns 404 if email not registered.")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request);
    }

    @PostMapping("/verify-otp")
    @SecurityRequirements({})
    @Operation(summary = "Verify a password-reset OTP",
            description = "Checks the OTP without consuming it — consumed only on POST /users/reset-password.")
    public ApiResponse<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return userService.verifyOtp(request);
    }

    @PostMapping("/reset-password")
    @SecurityRequirements({})
    @Operation(summary = "Reset password with a verified OTP",
            description = "Re-validates OTP, updates BCrypt password, consumes OTP.")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request);
    }

    // ─── Authenticated (any role) ────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user",
            description = "Returns lightweight identity {id, email, role, profileCompleted} from JWT.")
    public ApiResponse<MeResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername());
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get the current user's full profile",
            description = "Returns complete profile for the profile page.")
    public ApiResponse<UserResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getMyProfile(userDetails.getUsername());
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the current user's profile",
            description = "Partially updates fullName, phone, dob, avatarUrl. Omitted fields are unchanged.")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateMe(userDetails.getUsername(), request);
    }

    @PatchMapping("/complete-profile")
    @Operation(summary = "Complete first-time onboarding (OAuth2 users)",
            description = "One-time onboarding for a Google user: sets fullName, phone, dob and password. " +
                    "Fails with PROFILE_ALREADY_COMPLETED if already done.")
    public ApiResponse<UserResponse> completeProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompleteProfileRequest request) {
        return userService.completeProfile(userDetails.getUsername(), request);
    }

    @PostMapping("/me/deactivate-request")
    @Operation(summary = "Request account deletion (30-day grace period)",
            description = "Marks account PENDING_DELETE; a daily job purges it after 30 days.")
    public ApiResponse<DeleteAccountResponse> requestDeleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.requestDeleteAccount(userDetails.getUsername());
    }

    @PostMapping("/me/restore")
    @Operation(summary = "Cancel account deletion and restore to ACTIVE",
            description = "Cancels a PENDING_DELETE within the 30-day window.")
    public ApiResponse<DeleteAccountResponse> restoreAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.restoreAccount(userDetails.getUsername());
    }

    @PostMapping("/me/change-password/init")
    @Operation(summary = "Start an in-app password change (step 1)",
            description = "Verifies current password and emails OTP. Enforces 7-day cool-down.")
    public ApiResponse<String> initChangePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordInitRequest request) {
        return userService.initChangePassword(userDetails.getUsername(), request);
    }

    @PostMapping("/me/change-password/confirm")
    @Operation(summary = "Confirm an in-app password change (step 2)",
            description = "Validates OTP, updates BCrypt password. Session stays active.")
    public ApiResponse<String> confirmChangePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordConfirmRequest request) {
        return userService.confirmChangePassword(userDetails.getUsername(), request);
    }

    // ─── Admin only ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (paginated)",
            description = "Paginated + sortable list. " +
                    "Params: page (0-based), size, sort (e.g. sort=createdAt,desc or sort=role.roleName,asc). " +
                    "Defaults: page=0, size=20, sort=createdAt,desc.")
    public ApiResponse<PagedResponse<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a user by ID", description = "Looks up a single user by UUID. Restricted to ADMIN.")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId);
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new admin account",
            description = "Creates a new user with the ADMIN role. Restricted to existing admins.")
    public ApiResponse<UserResponse> registerAdmin(@Valid @RequestBody UserRegisterRequest request) {
        return userService.registerAdmin(request);
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock or unlock a user account",
            description = "Toggles the target user's status between ACTIVE and LOCKED. " +
                    "Admins cannot lock themselves or another ADMIN.")
    public ApiResponse<Void> toggleUserStatus(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.toggleUserStatus(userId, userDetails.getUsername());
    }

    @PatchMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a user (admin)",
            description = "Partially updates fullName, role (USER/ADMIN) and status (ACTIVE/LOCKED). " +
                    "An admin cannot change their own role or status.")
    public ApiResponse<UserResponse> adminUpdateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.adminUpdateUser(userId, request, userDetails.getUsername());
    }

    @PostMapping("/admin/{userId}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a user (admin)",
            description = "Marks the target user PENDING_DELETE with a 30-day grace period. " +
                    "An admin cannot delete themselves or another ADMIN.")
    public ApiResponse<UserResponse> adminSoftDeleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.adminSoftDeleteUser(userId, userDetails.getUsername());
    }

    @PostMapping("/admin/{userId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore a soft-deleted user (admin)",
            description = "Cancels a PENDING_DELETE and sets the account back to ACTIVE.")
    public ApiResponse<UserResponse> adminRestoreUser(@PathVariable UUID userId) {
        return userService.adminRestoreUser(userId);
    }

    @PostMapping("/admin/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset a user's password (admin)",
            description = "Sets a new BCrypt password for the target user (no OTP). " +
                    "All of the target user's sessions are revoked, forcing re-login.")
    public ApiResponse<Void> adminSetPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminSetPasswordRequest request) {
        return userService.adminSetPassword(userId, request);
    }

    @PostMapping(value = "/admin/{userId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload/replace a user's avatar (admin)",
            description = "Uploads an image (jpg/png/webp, ≤ 2 MB) to the target user's folder in the public " +
                    "'avatars' bucket, updates their avatar_url, and removes the previous avatar file.")
    public ApiResponse<UserResponse> adminUpdateAvatar(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file) {
        return userService.adminUpdateAvatar(userId, file);
    }
}