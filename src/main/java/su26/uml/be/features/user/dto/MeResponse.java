package su26.uml.be.features.user.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "MeResponse", description = "Identity of the currently authenticated user.")
public class MeResponse {
    @Schema(description = "Unique user identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID id;

    @Schema(description = "Email address.", example = "john.doe@gmail.com")
    String email;

    @Schema(description = "Role name assigned to the user.", example = "USER")
    String role;

    @Schema(description = "Avatar image URL.", example = "https://xyz.supabase.co/storage/v1/object/public/avatars/...")
    String avatarUrl;

    @Schema(description = "Whether the user has finished onboarding (set a real password). " +
            "Google users start as false and must complete the onboarding wizard.", example = "true")
    Boolean profileCompleted;

    @Schema(description = "ID of the plan the user is currently subscribed to (null = free/no subscription).",
            example = "11111111-1111-1111-1111-111111111111")
    UUID currentPlanId;

    // ─── Gói hiệu lực để FE vẽ tag/badge (resolve theo pattern 2B: paid sub → gói đó, không → base) ───
    @Schema(description = "ID gói hiệu lực — LUÔN có giá trị (free → id gói base). FE dùng highlight Pricing.",
            example = "11111111-1111-1111-1111-111111111111")
    UUID effectivePlanId;

    @Schema(description = "Tên gói hiệu lực để hiển thị tag/badge.", example = "Free")
    String planName;

    @Schema(description = "Màu tag của gói hiệu lực (optional).", example = "#7C3AED")
    String planColor;
}