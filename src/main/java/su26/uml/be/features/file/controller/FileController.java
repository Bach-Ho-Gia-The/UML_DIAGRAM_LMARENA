package su26.uml.be.features.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.common.config.SwaggerExamples;
import su26.uml.be.common.response.ApiResponse;
import su26.uml.be.features.file.dto.FileUploadResponse;
import su26.uml.be.features.file.dto.SignedUrlResponse;
import su26.uml.be.infrastructure.storage.StorageService;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Files", description = "Upload avatars/PDFs to Supabase Storage and generate signed URLs.")
public class FileController {

    StorageService storageService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload the current user's avatar",
            description = "Uploads an image (jpg/png/webp, max 2 MB) to the public 'avatars' bucket and " +
                    "returns its public URL. The file is stored at {userId}/{uuid}_{filename}."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Avatar uploaded.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.UPLOAD_AVATAR_RESPONSE)))
    public ApiResponse<FileUploadResponse> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Image file (jpg/png/webp, ≤ 2 MB)") @RequestParam("file") MultipartFile file) {

        return ApiResponse.success("Tải ảnh đại diện thành công",
                storageService.uploadAvatar(file, userDetails.getUsername()));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a private PDF document",
            description = "Uploads a PDF (max 10 MB) to the private 'documents' bucket and returns the storage " +
                    "path to persist in the DB. The file is NOT publicly accessible — use GET /files/documents/signed-url " +
                    "to obtain a temporary link."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Document uploaded.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.UPLOAD_DOCUMENT_RESPONSE)))
    public ApiResponse<FileUploadResponse> uploadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "PDF file (≤ 10 MB)") @RequestParam("file") MultipartFile file) {

        return ApiResponse.success("Tải tài liệu thành công",
                storageService.uploadDocument(file, userDetails.getUsername()));
    }

    @GetMapping("/documents/signed-url")
    @Operation(
            summary = "Get a signed URL for a private PDF",
            description = "Generates a time-limited signed URL to view/download a private document stored in the " +
                    "'documents' bucket. Provide the storage path returned by the upload endpoint."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Signed URL created.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(value = SwaggerExamples.SIGNED_URL_RESPONSE)))
    public ApiResponse<SignedUrlResponse> getDocumentSignedUrl(
            @Parameter(description = "Storage path inside the documents bucket ({userId}/{uuid}_{filename}).")
            @RequestParam("path") String path,
            @Parameter(description = "URL lifetime in seconds (default 3600).")
            @RequestParam(value = "expiresIn", defaultValue = "3600") int expiresIn) {

        return ApiResponse.success("Tạo đường dẫn truy cập thành công",
                storageService.getSignedUrl(path, expiresIn));
    }
}