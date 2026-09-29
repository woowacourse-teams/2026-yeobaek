package yeobaek.backend.admin.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlResponse;
import yeobaek.backend.support.storage.S3StorageProperties;
import yeobaek.backend.support.InvalidRequestException;

@Service
@Slf4j
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
        log.atInfo().addKeyValue(OPERATION, "admin.bookCover.issueUploadUrl").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue("contentTypeProvided", request.contentType() != null)
                .addKeyValue("contentLength", request.contentLength())
                .log("표지 업로드 URL을 발급합니다.");
        String extension = extensionOf(request.contentType());
        validateContentLength(request.contentLength());
        String key = properties.prefix() + "/book-covers/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(request.contentType())
                .cacheControl(CACHE_CONTROL)
                .build();
        PresignedPutObjectRequest presigned = presign(putObjectRequest);
        var response = new BookCoverUploadUrlResponse(key, presigned.url().toString(), presigned.expiration(),
                requiredHeaders(presigned, request.contentType()));
        log.atInfo().addKeyValue(OPERATION, "admin.bookCover.issueUploadUrl").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("contentType", request.contentType()).addKeyValue("contentLength", request.contentLength())
                .log("표지 업로드 URL을 발급했습니다.");
        return response;
    }

    private PresignedPutObjectRequest presign(PutObjectRequest putObjectRequest) {
        log.atInfo().addKeyValue(OPERATION, "s3.presignPutObject").addKeyValue(PHASE, ATTEMPT)
                .log("S3 서명 URL 발급 요청을 시작합니다.");
        try {
            var response = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(UPLOAD_URL_TTL)
                    .putObjectRequest(putObjectRequest)
                    .build());
            log.atInfo().addKeyValue(OPERATION, "s3.presignPutObject").addKeyValue(PHASE, SUCCESS)
                    .log("S3 서명 URL 발급 요청을 완료했습니다.");
            return response;
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

    private String extensionOf(String contentType) {
        if (contentType == null) {
            throw new InvalidRequestException("표지 이미지 MIME 타입은 필수입니다.",
                    Map.of(REASON, "content_type_missing"));
        }
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new InvalidRequestException("표지 이미지는 JPEG, PNG, WebP 형식만 허용합니다.",
                    Map.of(REASON, "content_type_unsupported"));
        }
        return extension;
    }

    private void validateContentLength(long contentLength) {
        if (contentLength < 1 || contentLength > MAX_CONTENT_LENGTH) {
            throw new InvalidRequestException("표지 이미지 크기는 1바이트 이상 5 MiB 이하여야 합니다.",
                    Map.of(REASON, "content_length_out_of_range", "contentLength", Long.toString(contentLength)));
        }
    }
}
