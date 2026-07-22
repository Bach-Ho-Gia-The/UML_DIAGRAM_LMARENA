package su26.uml.be.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;
import su26.uml.be.features.file.dto.FileUploadResponse;
import su26.uml.be.features.file.dto.SignedUrlResponse;

public interface StorageService {
    FileUploadResponse uploadAvatar(MultipartFile file, String email);
    /** Upload an avatar into a specific user's folder (admin managing another user). */
    FileUploadResponse uploadAvatarForUser(MultipartFile file, String userId);
    FileUploadResponse uploadDocument(MultipartFile file, String email);

    String uploadAvatarFromUrl(String imageUrl, String userId);

    SignedUrlResponse getSignedUrl(String path, int expiresInSeconds);
    void deleteFile(String bucket, String path);
}