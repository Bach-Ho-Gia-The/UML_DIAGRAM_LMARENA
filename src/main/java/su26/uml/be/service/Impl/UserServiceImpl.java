package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su26.uml.be.annotation.Auditable;
import su26.uml.be.audit.AuditContext;
import su26.uml.be.dto.request.*;
import su26.uml.be.enums.UserStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.enums.PlanStatus;
import su26.uml.be.dto.response.ApiResponse;
import su26.uml.be.dto.response.DeleteAccountResponse;
import su26.uml.be.dto.response.MeResponse;
import su26.uml.be.dto.response.PagedResponse;
import su26.uml.be.dto.response.UserResponse;

import java.time.LocalDateTime;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.Role;
import su26.uml.be.entity.Subscription;
import su26.uml.be.entity.User;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.mapper.UserMapper;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.repository.RoleRepository;
import su26.uml.be.repository.SheetRepository;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.dto.response.FileUploadResponse;
import su26.uml.be.service.EmailService;
import su26.uml.be.service.OtpService;
import su26.uml.be.service.RefreshTokenService;
import su26.uml.be.service.StorageService;
import su26.uml.be.service.UserService;


import java.security.SecureRandom;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    PlanRepository planRepository;
    SubscriptionRepository subscriptionRepository;
    ProjectRepository projectRepository;
    SheetRepository sheetRepository;

    EmailService emailService;
    OtpService otpService;
    RefreshTokenService refreshTokenService;
    StorageService storageService;

    UserMapper userMapper;

    PasswordEncoder passwordEncoder;

    static SecureRandom SECURE_RANDOM = new SecureRandom();
    static int PASSWORD_CHANGE_COOLDOWN_DAYS = 7;

    @Override
    public ApiResponse<UserResponse> registerUser(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.EMAIL_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.DEFAULT_ROLE_NOT_FOUND));
        user.setRole(role);

        user.setStatus(UserStatus.ACTIVE);
        user.setProfileCompleted(true); // tài khoản đăng ký thường đã có đủ thông tin
        User savedUser = userRepository.save(user);
        assignLowestPlan(savedUser);

        UserResponse userResponse = userMapper.toUserResponse(savedUser);
//        resolveAvatar(userResponse);
        return ApiResponse.success("Đăng ký tài khoản thành công", userResponse);
    }

    @Override
    public ApiResponse<UserResponse> updateMe(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(request, user);
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Cập nhật thông tin thành công", userMapper.toUserResponse(savedUser));
    }

    @Override
    public ApiResponse<UserResponse> completeProfile(String email, CompleteProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Onboarding is one-time only: refuse if the profile is already completed.
        if (Boolean.TRUE.equals(user.getProfileCompleted())) {
            throw new AppException(ErrorCode.PROFILE_ALREADY_COMPLETED);
        }

        // Never trust the FE: re-validate password match + strength on the server.
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORDS_NOT_MATCH);
        }
        if (passwordStrength(request.getPassword()) < MIN_PASSWORD_STRENGTH) {
            throw new AppException(ErrorCode.WEAK_PASSWORD);
        }

        userMapper.completeProfile(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        user.setProfileCompleted(true); // đánh dấu đã hoàn tất onboarding
        User savedUser = userRepository.save(user);

        // Confirmation email — never includes the plaintext password.
        try {
            emailService.sendAccountSetupSuccessEmail(
                    savedUser.getEmail(), savedUser.getFullName(), savedUser.getLastPasswordChangeAt());
        } catch (Exception e) {
            // Email is best-effort: onboarding must still succeed if SMTP hiccups.
            log.warn("Không thể gửi email xác nhận thiết lập tài khoản cho {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return ApiResponse.success("Đã lưu thông tin, email xác nhận đã được gửi",
                userMapper.toUserResponse(savedUser));
    }

    // Minimum acceptable strength = "Trung bình" (3 of 5 criteria), matching the FE meter.
    private static final int MIN_PASSWORD_STRENGTH = 3;

    /** Count satisfied criteria: length>=8, uppercase, lowercase, digit, special char. */
    private int passwordStrength(String pw) {
        if (pw == null) return 0;
        int score = 0;
        if (pw.length() >= 8) score++;
        if (pw.matches(".*[A-Z].*")) score++;
        if (pw.matches(".*[a-z].*")) score++;
        if (pw.matches(".*\\d.*")) score++;
        if (pw.matches(".*[^A-Za-z0-9].*")) score++;
        return score;
    }

    @Override
    public ApiResponse<DeleteAccountResponse> requestDeleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() == UserStatus.PENDING_DELETE)
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_PENDING_DELETE);

        if (user.getStatus() == UserStatus.LOCKED)
            throw new AppException(ErrorCode.USER_INACTIVE);

        LocalDateTime deletionDate = LocalDateTime.now().plusDays(30);
        user.setStatus(UserStatus.PENDING_DELETE);
        user.setDeletionDate(deletionDate);
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Yêu cầu xóa tài khoản đã được ghi nhận",
                userMapper.toDeleteAccountResponse(savedUser, 30L,
                        "Tài khoản sẽ bị xóa vĩnh viễn sau 30 ngày. Bạn có thể khôi phục trước thời hạn này."));
    }

    @Override
    public ApiResponse<DeleteAccountResponse> restoreAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.PENDING_DELETE)
            throw new AppException(ErrorCode.ACCOUNT_NOT_PENDING_DELETE);

        user.setStatus(UserStatus.ACTIVE);
        user.setDeletionDate(null);
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Tài khoản đã được khôi phục thành công",
                userMapper.toDeleteAccountResponse(savedUser, null,
                        "Tài khoản của bạn đã được khôi phục và hoạt động bình thường."));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<UserResponse>> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);

        if (page.isEmpty()) {
            throw new AppException(ErrorCode.USER_LIST_EMPTY);
        }

        List<User> users = page.getContent();
        List<UserResponse> responses = userMapper.toUserResponseList(users);

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            UserResponse resp = responses.get(i);

            resp.setLastActiveAt(user.getLastActiveAt());

            long projectCount = projectRepository.countByUserAndIsDeletedFalse(user);
            resp.setProjectCount(projectCount);

            long diagramCount = projectCount > 0
                    ? sheetRepository.countByProject_UserAndProject_IsDeletedFalse(user)
                    : 0L;
            resp.setDiagramCount(diagramCount);
        }

        PagedResponse<UserResponse> pagedResponse = PagedResponse.<UserResponse>builder()
                .content(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ApiResponse.success("Lấy danh sách người dùng thành công", pagedResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserResponse userResponse = userMapper.toUserResponse(user);
//        resolveAvatar(userResponse);
        return ApiResponse.success("Lấy thông tin Người dùng thành công", userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<MeResponse> getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        MeResponse meResponse = userMapper.toMeResponse(user);

        return ApiResponse.success("Lấy thông tin người dùng hiện tại thành công", meResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserResponse userResponse = userMapper.toUserResponse(user);

        return ApiResponse.success("Lấy hồ sơ cá nhân thành công", userResponse);
    }

    @Override
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        String otpCode = generateOtp();

        // Lưu OTP (đã hash) trong Redis kèm TTL — thay cho bảng password_reset_tokens.
        otpService.storeOtp(user.getEmail(), otpCode);
        emailService.sendForgotPasswordOtpEmail(user.getEmail(), otpCode, user.getFullName());
        return ApiResponse.success("Mã OTP đã được gửi đến email của bạn", null);
    }

    @Override
    public ApiResponse<String> verifyOtp(VerifyOtpRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        // Ném AppException nếu OTP sai/hết hạn/vượt số lần thử.
        otpService.verifyOtp(request.getEmail(), request.getOtpCode());
        otpService.markVerified(request.getEmail());

        return ApiResponse.success("Mã OTP hợp lệ");
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORDS_NOT_MATCH);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        // Chỉ cho đổi mật khẩu khi OTP đã được verify ở bước trước (cờ ngắn hạn trong Redis).
        if (!otpService.isVerified(request.getEmail())) {
            throw new AppException(ErrorCode.OTP_NOT_VERIFIED);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userRepository.save(user);

        // OTP dùng một lần: xoá toàn bộ trạng thái OTP sau khi đổi thành công.
        otpService.invalidate(request.getEmail());

        // Vô hiệu hoá mọi phiên đăng nhập cũ sau khi đổi mật khẩu.
        refreshTokenService.revokeAllTokens(user.getId().toString());
        refreshTokenService.setLogoutTime(user.getEmail());

        return ApiResponse.success("Đặt lại mật khẩu thành công");
    }

    @Override
    @Transactional
    public ApiResponse<String> initChangePassword(String email, ChangePasswordInitRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Người dùng phải có mật khẩu hiện tại để xác minh (Google user chưa onboarding sẽ không có).
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        // Giới hạn tần suất: chỉ cho đổi mật khẩu 1 lần trong 7 ngày.
        if (user.getLastPasswordChangeAt() != null
                && user.getLastPasswordChangeAt().isAfter(
                        LocalDateTime.now().minusDays(PASSWORD_CHANGE_COOLDOWN_DAYS))) {
            throw new AppException(ErrorCode.PASSWORD_CHANGE_LIMIT);
        }

        String otpCode = generateOtp();
        // Tái dùng OtpService (Redis, đã hash + TTL 90s) như luồng quên mật khẩu.
        otpService.storeOtp(user.getEmail(), otpCode);
        emailService.sendChangePasswordOtpEmail(user.getEmail(), otpCode, user.getFullName());

        return ApiResponse.success("Mã OTP đổi mật khẩu đã được gửi đến email của bạn");
    }

    @Override
    @Transactional
    public ApiResponse<String> confirmChangePassword(String email, ChangePasswordConfirmRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Không tin FE: xác thực lại khớp mật khẩu + độ mạnh ở phía server.
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORDS_NOT_MATCH);
        }
        if (request.getNewPassword().length() < 8
                || passwordStrength(request.getNewPassword()) < MIN_PASSWORD_STRENGTH) {
            throw new AppException(ErrorCode.WEAK_PASSWORD);
        }

        // Xác thực OTP (ném AppException nếu sai/hết hạn/vượt số lần thử).
        otpService.verifyOtp(user.getEmail(), request.getOtpCode());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userRepository.save(user);

        // OTP dùng một lần: xoá toàn bộ trạng thái OTP sau khi đổi thành công.
        otpService.invalidate(user.getEmail());

        return ApiResponse.success("Đổi mật khẩu thành công");
    }


    @Override
    @Auditable(action = "ADMIN_CREATE", targetType = "USER")
    public ApiResponse<UserResponse> registerAdmin(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.EMAIL_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setProfileCompleted(true);
        User savedUser = userRepository.save(user);
        assignLowestPlan(savedUser);

        // Audit: id/email của admin vừa tạo (target chỉ có sau khi save).
        AuditContext.setTargetId(savedUser.getId());
        AuditContext.putDetail("email", savedUser.getEmail());
        AuditContext.putDetail("role", "ADMIN");

        return ApiResponse.success("Tạo tài khoản Admin thành công", userMapper.toUserResponse(savedUser));
    }

    @Override
    @Auditable(action = "USER_STATUS_TOGGLE", targetType = "USER", targetId = "#userId")
    public ApiResponse<Void> toggleUserStatus(UUID userId, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getId().equals(currentUser.getId()))
            throw new AppException(ErrorCode.DELETE_SELF_INVALID);

        if ("ADMIN".equalsIgnoreCase(user.getRole().getRoleName()))
            throw new AppException(ErrorCode.DELETE_OTHER_ADMIN_INVALID);

        UserStatus before = user.getStatus();
        String message;
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.LOCKED);
            message = "Đã khóa tài khoản thành công";
            // Toggle: chọn action cụ thể để FE lọc đúng (thay cho action tĩnh của annotation).
            AuditContext.setAction("USER_LOCK");
        } else {
            user.setStatus(UserStatus.ACTIVE);
            message = "Đã mở khóa tài khoản thành công";
            AuditContext.setAction("USER_UNLOCK");
        }
        userRepository.save(user);

        AuditContext.putBeforeAfter(before, user.getStatus());

        return ApiResponse.success(message, null);
    }

    @Override
    @Auditable(action = "USER_UPDATE", targetType = "USER", targetId = "#userId")
    public ApiResponse<UserResponse> adminUpdateUser(UUID userId, AdminUpdateUserRequest request, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isSelf = user.getId().equals(currentUser.getId());

        // Họ tên: luôn cho phép cập nhật.
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        // Thông tin cá nhân: cho phép cập nhật (bỏ qua nếu không gửi).
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        // Vai trò: admin không được tự đổi vai trò của chính mình (tránh tự hạ quyền).
        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            if (isSelf) throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
            Role role = roleRepository.findByRoleName(request.getRoleName().toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            user.setRole(role);
        }

        // Trạng thái: admin không được tự khóa chính mình. Chỉ nhận ACTIVE/LOCKED tại đây
        // (PENDING_DELETE đi qua endpoint soft-delete riêng).
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            if (isSelf) throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
            UserStatus target;
            try {
                target = UserStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_STATUS);
            }
            if (target != UserStatus.ACTIVE && target != UserStatus.LOCKED) {
                throw new AppException(ErrorCode.INVALID_STATUS);
            }
            user.setStatus(target);
            if (target == UserStatus.ACTIVE) user.setDeletionDate(null);
        }

        User savedUser = userRepository.save(user);
        return ApiResponse.success("Cập nhật người dùng thành công", userMapper.toUserResponse(savedUser));
    }

    @Override
    @Auditable(action = "USER_SOFT_DELETE", targetType = "USER", targetId = "#userId")
    public ApiResponse<UserResponse> adminSoftDeleteUser(UUID userId, String currentUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Không cho tự xóa chính mình, và không cho xóa Quản trị viên khác.
        if (user.getId().equals(currentUser.getId()))
            throw new AppException(ErrorCode.DELETE_SELF_INVALID);
        if ("ADMIN".equalsIgnoreCase(user.getRole().getRoleName()))
            throw new AppException(ErrorCode.DELETE_OTHER_ADMIN_INVALID);
        if (user.getStatus() == UserStatus.PENDING_DELETE)
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_PENDING_DELETE);

        user.setStatus(UserStatus.PENDING_DELETE);
        user.setDeletionDate(LocalDateTime.now().plusDays(30));
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Đã chuyển tài khoản sang trạng thái chờ xoá (30 ngày)",
                userMapper.toUserResponse(savedUser));
    }

    @Override
    @Auditable(action = "USER_RESTORE", targetType = "USER", targetId = "#userId")
    public ApiResponse<UserResponse> adminRestoreUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getStatus() != UserStatus.PENDING_DELETE)
            throw new AppException(ErrorCode.ACCOUNT_NOT_PENDING_DELETE);

        user.setStatus(UserStatus.ACTIVE);
        user.setDeletionDate(null);
        User savedUser = userRepository.save(user);

        return ApiResponse.success("Khôi phục tài khoản thành công", userMapper.toUserResponse(savedUser));
    }

    @Override
    @Auditable(action = "USER_PASSWORD_RESET", targetType = "USER", targetId = "#userId")
    public ApiResponse<Void> adminSetPassword(UUID userId, AdminSetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Không tin FE: xác thực lại độ mạnh (đã có @Pattern ở DTO nhưng vẫn guard).
        if (passwordStrength(request.getNewPassword()) < 2) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userRepository.save(user);

        // Admin đặt lại mật khẩu → thu hồi mọi phiên cũ, buộc user đăng nhập lại.
        refreshTokenService.revokeAllTokens(user.getId().toString());
        refreshTokenService.setLogoutTime(user.getEmail());

        return ApiResponse.success("Đặt lại mật khẩu cho người dùng thành công", null);
    }

    @Override
    @Auditable(action = "USER_AVATAR_UPDATE", targetType = "USER", targetId = "#userId")
    public ApiResponse<UserResponse> adminUpdateAvatar(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String oldAvatarUrl = user.getAvatarUrl();

        // Upload ảnh mới vào đúng thư mục của user đích rồi lưu URL vào DB.
        FileUploadResponse uploaded = storageService.uploadAvatarForUser(file, userId.toString());
        user.setAvatarUrl(uploaded.getUrl());
        userRepository.save(user);

        // Dọn file cũ trong bucket avatars (best-effort — không chặn luồng nếu lỗi).
        deleteAvatarQuietly(oldAvatarUrl, uploaded.getUrl());

        return ApiResponse.success("Cập nhật ảnh đại diện thành công", userMapper.toUserResponse(user));
    }

    /** Xoá file avatar cũ trong bucket 'avatars' nếu URL trỏ về Supabase Storage của ta. */
    private void deleteAvatarQuietly(String oldUrl, String newUrl) {
        if (oldUrl == null || oldUrl.isBlank() || oldUrl.equals(newUrl)) return;
        String marker = "/object/public/avatars/";
        int idx = oldUrl.indexOf(marker);
        if (idx < 0) return; // không phải file trong Storage của ta (vd. Google avatar) → bỏ qua
        String path = oldUrl.substring(idx + marker.length());
        try {
            storageService.deleteFile("avatars", path);
        } catch (Exception e) {
            log.warn("Không xoá được avatar cũ '{}': {}", path, e.getMessage());
        }
    }

    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private void assignLowestPlan(User user) {
        planRepository.findFirstByIsBasePlanTrueAndStatus(PlanStatus.ACTIVE)
                .or(() -> planRepository.findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE))
                .ifPresentOrElse(plan -> {
                    boolean isBase = Boolean.TRUE.equals(plan.getIsBasePlan())
                            || (plan.getTierOrder() != null && plan.getTierOrder() == 0)
                            || plan.getPrice().signum() == 0;
                    if (isBase) {
                        user.setCurrentSubscription(null);
                    } else {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime endDate = plan.getDurationDays() != null && plan.getDurationDays() > 0
                                ? now.plusDays(plan.getDurationDays())
                                : null;

                        Subscription subscription = Subscription.builder()
                                .user(user)
                                .plan(plan)
                                .status(SubscriptionStatus.ACTIVE)
                                .startDate(now)
                                .endDate(endDate)
                                .build();

                        subscriptionRepository.save(subscription);
                        user.setCurrentSubscription(subscription);
                    }
                }, () -> log.warn("Không tìm thấy gói ACTIVE nào — user {} không được gán subscription", user.getEmail()));
    }
}
