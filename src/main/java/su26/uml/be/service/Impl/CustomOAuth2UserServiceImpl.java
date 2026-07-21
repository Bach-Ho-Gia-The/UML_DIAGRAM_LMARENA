package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import su26.uml.be.security.CustomOAuth2User;
import su26.uml.be.dto.request.OAuth2UserInfo;
import su26.uml.be.entity.Plan;
import su26.uml.be.entity.Role;
import su26.uml.be.entity.Subscription;
import su26.uml.be.entity.User;
import su26.uml.be.enums.PlanStatus;
import su26.uml.be.enums.SubscriptionStatus;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.mapper.OAuth2UserMapper;
import su26.uml.be.repository.PlanRepository;
import su26.uml.be.repository.RoleRepository;
import su26.uml.be.repository.SubscriptionRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.enums.UserStatus;
import su26.uml.be.service.CustomOAuth2UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomOAuth2UserServiceImpl extends DefaultOAuth2UserService implements CustomOAuth2UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PlanRepository planRepository;
    SubscriptionRepository subscriptionRepository;
    OAuth2UserMapper oauth2UserMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        try {
            return processOAuth2User(oAuth2User);
        } catch (AppException e) {
            log.warn("OAuth2 xử lý thất bại: {}", e.getErrorCode().getMessage());
            throw new OAuth2AuthenticationException(e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("Lỗi không xác định khi xử lý OAuth2 user", e);
            throw new OAuth2AuthenticationException(ErrorCode.OAUTH2_PROCESSING_ERROR.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            throw new AppException(ErrorCode.OAUTH2_EMAIL_NOT_VERIFIED);
        }

        String googleId = (String) attributes.get("sub");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        User user = userRepository.findByEmail(email)
                .map(existing -> linkGoogleAccount(existing, googleId, picture))
                .orElseGet(() -> createGoogleUser(email, googleId, name, picture));

        return new CustomOAuth2User(Collections.emptyList(), attributes, "email", user);
    }

    private User linkGoogleAccount(User user, String googleId, String picture) {
        OAuth2UserInfo info = OAuth2UserInfo.builder()
                .googleId(user.getGoogleId() == null ? googleId : null)
                .provider(!"GOOGLE".equals(user.getProvider()) ? "GOOGLE" : null)
                .avatarUrl(picture != null && user.getAvatarUrl() == null ? picture : null)
                .build();

        oauth2UserMapper.updateGoogleFields(info, user);

        boolean hasChanges = info.getGoogleId() != null
                || info.getProvider() != null
                || info.getAvatarUrl() != null;

        return hasChanges ? userRepository.save(user) : user;
    }

    private User createGoogleUser(String email, String googleId, String name, String picture) {
        Role defaultRole = roleRepository.findByRoleName("USER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("USER")
                                .description("Người dùng thông thường")
                                .build()
                ));

        String baseUsername = email.split("@")[0];
        String username = userRepository.existsByUsername(baseUsername)
                ? baseUsername + "_" + googleId.substring(0, 6)
                : baseUsername;

        OAuth2UserInfo info = OAuth2UserInfo.builder()
                .username(username)
                .email(email)
                .fullName(name != null ? name : baseUsername)
                .googleId(googleId)
                .provider("GOOGLE")
                .avatarUrl(picture)
                .status(UserStatus.ACTIVE)
                .build();

        User newUser = oauth2UserMapper.toUser(info);
        newUser.setRole(defaultRole);
        newUser.setProfileCompleted(false); // Google user lần đầu: phải qua onboarding

        User saved = userRepository.save(newUser);
        assignLowestPlan(saved);
        log.info("Tạo user mới từ Google OAuth2: {}", email);
        return saved;
    }

    private void assignLowestPlan(User user) {
        planRepository.findFirstByStatusOrderByPriceAscCreatedAtAsc(PlanStatus.ACTIVE)
                .ifPresentOrElse(plan -> {
                    boolean isBase = plan.getTierOrder() == null || plan.getTierOrder() == 0;
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
