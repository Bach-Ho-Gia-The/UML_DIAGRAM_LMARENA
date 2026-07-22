package su26.uml.be.features.user.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.features.ai.dto.*;
import su26.uml.be.features.user.dto.*;
import su26.uml.be.features.admin.dto.AdminSetPasswordRequest;
import su26.uml.be.features.admin.dto.AdminUpdateUserRequest;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.user.dto.DeleteAccountResponse;
import su26.uml.be.features.user.dto.MeResponse;
import su26.uml.be.common.response.PagedResponse;
import su26.uml.be.features.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {
    ApiResponse<UserResponse> registerUser(UserRegisterRequest request);
    ApiResponse<UserResponse> updateMe(String email, UpdateUserRequest request);
    ApiResponse<UserResponse> completeProfile(String email, CompleteProfileRequest request);
    ApiResponse<DeleteAccountResponse> requestDeleteAccount(String email);
    ApiResponse<DeleteAccountResponse> restoreAccount(String email);
    ApiResponse<PagedResponse<UserResponse>> getAllUsers(Pageable pageable);
    ApiResponse<UserResponse> getUserById(UUID userId);
    ApiResponse<MeResponse> getCurrentUser(String email);
    ApiResponse<UserResponse> getMyProfile(String email);

    ApiResponse<String> forgotPassword(ForgotPasswordRequest request);
    ApiResponse<String> verifyOtp(VerifyOtpRequest request);
    ApiResponse<String> resetPassword(ResetPasswordRequest request);
    ApiResponse<String> initChangePassword(String email, ChangePasswordInitRequest request);
    ApiResponse<String> confirmChangePassword(String email, ChangePasswordConfirmRequest request);

    ApiResponse<UserResponse> registerAdmin(UserRegisterRequest request);
    ApiResponse<Void> toggleUserStatus(UUID userId, String currentUserEmail);

    // Admin user management (full CRUD)
    ApiResponse<UserResponse> adminUpdateUser(UUID userId, AdminUpdateUserRequest request, String currentUserEmail);
    ApiResponse<UserResponse> adminSoftDeleteUser(UUID userId, String currentUserEmail);
    ApiResponse<UserResponse> adminRestoreUser(UUID userId);
    ApiResponse<Void> adminSetPassword(UUID userId, AdminSetPasswordRequest request);
    ApiResponse<UserResponse> adminUpdateAvatar(UUID userId, MultipartFile file);
}