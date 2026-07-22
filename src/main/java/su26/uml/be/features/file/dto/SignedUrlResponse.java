package su26.uml.be.features.file.dto;

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
@Schema(name = "SignedUrlResponse", description = "A time-limited signed URL for a private Storage object.")
public class SignedUrlResponse {

    @Schema(description = "Temporary signed URL to view/download the private file.",
            example = "https://xyz.supabase.co/storage/v1/object/sign/documents/5b2e.../doc.pdf?token=...")
    String signedUrl;

    @Schema(description = "Lifetime of the signed URL in seconds.", example = "3600")
    int expiresInSeconds;
}