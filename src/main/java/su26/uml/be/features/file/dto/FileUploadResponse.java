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
@Schema(name = "FileUploadResponse", description = "Result of a file upload to Supabase Storage.")
public class FileUploadResponse {

    @Schema(description = "Bucket the file was stored in.", example = "avatars")
    String bucket;

    @Schema(description = "Storage path inside the bucket ({userId}/{uuid}_{filename}). Persist this in the DB.",
            example = "5b2e.../9f1c2a_avatar.png")
    String path;

    @Schema(description = "Public URL — present only for public buckets (e.g. avatars); null for private buckets.",
            example = "https://xyz.supabase.co/storage/v1/object/public/avatars/5b2e.../9f1c2a_avatar.png")
    String url;
}