package yeobaek.backend.admin.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlResponse;
import yeobaek.backend.support.storage.S3StorageProperties;

@Service
public class BookCoverUploadService {

    public static final long MAX_CONTENT_LENGTH = 5L * 1024 * 1024;
    public static final String CACHE_CONTROL = "public,max-age=31536000,immutable";
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public BookCoverUploadService(S3Presigner s3Presigner, S3StorageProperties properties) {
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public BookCoverUploadUrlResponse issueUploadUrl(BookCoverUploadUrlRequest request) {
        String extension = extensionOf(request.contentType());
        validateContentLength(request.contentLength());
        String key = "book-covers/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(request.contentType())
                .cacheControl(CACHE_CONTROL)
                .build();
        PresignedPutObjectRequest presigned = presign(putObjectRequest);
        return new BookCoverUploadUrlResponse(key, presigned.url().toString(), presigned.expiration(),
                requiredHeaders(presigned, request.contentType()));
    }

    private PresignedPutObjectRequest presign(PutObjectRequest putObjectRequest) {
        try {
            return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(UPLOAD_URL_TTL)
                    .putObjectRequest(putObjectRequest)
                    .build());
        } catch (SdkException exception) {
            throw new IllegalStateException("표지 이미지 업로드 URL 발급에 실패했습니다.", exception);
        }
    }

    private Map<String, String> requiredHeaders(PresignedPutObjectRequest presigned, String contentType) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Cache-Control", CACHE_CONTROL);
        presigned.signedHeaders().forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name)
                    && !"content-type".equalsIgnoreCase(name)
                    && !"cache-control".equalsIgnoreCase(name)) {
                headers.put(name, String.join(",", values));
            }
        });
        return headers;
    }

    private static String extensionOf(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("표지 이미지 MIME 타입은 필수입니다.");
        }
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("표지 이미지는 JPEG, PNG, WebP 형식만 허용합니다.");
        }
        return extension;
    }

    private static void validateContentLength(long contentLength) {
        if (contentLength < 1 || contentLength > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("표지 이미지 크기는 1바이트 이상 5 MiB 이하여야 합니다.");
        }
    }
}
