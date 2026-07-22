package su26.uml.be.features.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "UpdateUserRequest", description = "Payload for updating the current user's profile. Only provided fields are updated.")
public class UpdateUserRequest {

    @Size(max = 255, message = "INVALID_FULLNAME")
    @Schema(description = "Display name, up to 255 characters.", example = "Nguyen Van B")
    String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "INVALID_PHONE")
    @Schema(description = "Phone number, 10-11 digits.", example = "0909999999")
    String phone;

    @Past(message = "INVALID_DOB")
    @Schema(description = "Date of birth, must be in the past.", example = "2000-01-15")
    LocalDate dob;

    @Size(max = 500, message = "INVALID_AVATAR_URL")
    @Schema(description = "Avatar image URL (typically the public URL returned by POST /files/avatar).",
            example = "https://xyz.supabase.co/storage/v1/object/public/avatars/<userId>/<uuid>_avatar.png")
    String avatarUrl;
}