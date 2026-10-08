package yeobaek.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlRequest;
import yeobaek.backend.admin.dto.BookCoverUploadUrlResponse;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.support.storage.S3StorageProperties;

class BookCoverUploadServiceTest {

    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
    private BookCoverUploadService service;

    @BeforeEach
    void setUp() {
        service = new BookCoverUploadService(s3Presigner,
                new S3StorageProperties("cover-bucket", "ap-northeast-2", "https://cover.example", "yeobaek"));
    }

    @Test
    @DisplayName("원본과 두 JPEG에 같은 UUID와 10분짜리 PUT URL 및 고정 캐시 헤더를 발급한다")
    void issuePresignedPutUrl() throws Exception {
        Instant expiration = Instant.parse("2026-08-26T12:10:00Z");
        stubSuccessfulPresign(expiration);

        BookCoverUploadUrlResponse response = service.issueUploadUrl(
                new BookCoverUploadUrlRequest("image/webp", 1024L, 256L));

        assertThat(response.coverImageKey()).matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(response.original())
                .extracting(BookCoverUploadUrlResponse.UploadTarget::uploadUrl, BookCoverUploadUrlResponse.UploadTarget::expiresAt)
                .containsExactly("https://s3.example/upload", expiration);
        assertThat(response.original().requiredHeaders()).containsEntry("Content-Type", "image/webp")
                .containsEntry("Cache-Control", BookCoverUploadService.CACHE_CONTROL);
    }

    @Test
    @DisplayName("PUT URL은 지정한 버킷·키·메타데이터로 10분 동안 서명한다")
    void configurePresignedPutRequest() throws Exception {
        stubSuccessfulPresign(Instant.parse("2026-08-26T12:10:00Z"));

        BookCoverUploadUrlResponse response = service.issueUploadUrl(
                new BookCoverUploadUrlRequest("image/webp", 1024L, 256L));

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner, times(3)).presignPutObject(captor.capture());
        PutObjectPresignRequest request = captor.getAllValues().getFirst();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(request.putObjectRequest())
                .extracting(put -> put.bucket(), put -> put.key(), put -> put.contentType(),
                        put -> put.cacheControl(), put -> put.contentLength())
                .containsExactly("cover-bucket", "yeobaek/book-covers/orgin/" + response.coverImageKey(), "image/webp",
                        BookCoverUploadService.CACHE_CONTROL, null);
    }

    @Test
    @DisplayName("표지 업로드 URL 완료 로그는 파일 형식과 크기를 포함하고 서명 URL을 제외한다")
    void logSafeUploadMetadata() throws Exception {
        stubSuccessfulPresign(Instant.parse("2026-08-26T12:10:00Z"));

        try (var logs = new LogCapture(BookCoverUploadService.class.getName())) {
            service.issueUploadUrl(new BookCoverUploadUrlRequest("image/webp", 1024L, 256L));

            var event = logs.event("admin.bookCover.issueUploadUrl", "success");
            assertThat(logs.field(event, "contentType")).isEqualTo("image/webp");
            assertThat(logs.field(event, "contentLength")).isEqualTo(1024L);
            assertThat(logs.structuredText()).doesNotContain("https://s3.example/upload");
        }
    }

    @Test
    @DisplayName("실제 Presigner가 요구하는 브라우저 PUT 헤더를 빠짐없이 응답한다")
    void exposeActualSignedHeaders() {
        try (S3Presigner realPresigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build()) {
            BookCoverUploadService realService = new BookCoverUploadService(realPresigner,
                    new S3StorageProperties(
                            "cover-bucket", "ap-northeast-2", "https://cover.example", "custom-prefix"));

            BookCoverUploadUrlResponse response = realService.issueUploadUrl(
                    new BookCoverUploadUrlRequest("image/jpeg", BookCoverUploadService.MAX_CONTENT_LENGTH, 256L));

            assertThat(response.original().uploadUrl()).startsWith(
                    "https://cover-bucket.s3.ap-northeast-2.amazonaws.com/custom-prefix/book-covers/orgin/");
            assertThat(response.original().requiredHeaders()).containsEntry("Content-Type", "image/jpeg")
                    .containsEntry("Cache-Control", BookCoverUploadService.CACHE_CONTROL);
            assertThat(response.original().requiredHeaders()).containsOnlyKeys("Content-Type", "Cache-Control");
        }
    }

    @Test
    @DisplayName("지원하지 않는 형식과 범위를 벗어난 크기는 URL 발급 전에 거부한다")
    void rejectInvalidFileMetadata() {
        assertThatThrownBy(() -> service.issueUploadUrl(new BookCoverUploadUrlRequest("image/gif", 1024L, 256L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issueUploadUrl(new BookCoverUploadUrlRequest(null, 1024L, 256L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issueUploadUrl(new BookCoverUploadUrlRequest("image/png", 0L, 256L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.issueUploadUrl(
                new BookCoverUploadUrlRequest("image/jpeg", BookCoverUploadService.MAX_CONTENT_LENGTH + 1, 256L)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("압축본과 구버전용 객체는 확장자 없는 같은 UUID를 쓰고 JPEG 메타데이터를 가진다")
    void issueJpegVariants() throws Exception {
        stubSuccessfulPresign(Instant.parse("2026-08-26T12:10:00Z"));
        var response = service.issueUploadUrl(new BookCoverUploadUrlRequest("image/png", 1024L, 256L));
        var captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner, times(3)).presignPutObject(captor.capture());
        assertThat(captor.getAllValues()).extracting(put -> put.putObjectRequest().key())
                .containsExactly("yeobaek/book-covers/orgin/" + response.coverImageKey(),
                        "yeobaek/book-covers/low/" + response.coverImageKey(),
                        "yeobaek/book-covers/" + response.coverImageKey());
        assertThat(captor.getAllValues()).extracting(put -> put.putObjectRequest().contentType())
                .containsExactly("image/png", "image/jpeg", "image/jpeg");
        assertThat(response.low().requiredHeaders()).containsEntry("Content-Type", "image/jpeg")
                .containsEntry("Cache-Control", BookCoverUploadService.CACHE_CONTROL);
        assertThat(response.legacy().requiredHeaders()).containsEntry("Content-Type", "image/jpeg")
                .containsEntry("Cache-Control", BookCoverUploadService.CACHE_CONTROL);
    }

    @Test
    @DisplayName("압축본 크기가 누락되거나 범위를 벗어나면 모든 서명을 시작하기 전에 거부한다")
    void rejectInvalidLowContentLength() {
        for (Long length : java.util.Arrays.asList(null, 0L, BookCoverUploadService.MAX_CONTENT_LENGTH + 1)) {
            assertThatThrownBy(() -> service.issueUploadUrl(new BookCoverUploadUrlRequest("image/png", 1024L, length)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(s3Presigner);
    }

    private void stubSuccessfulPresign(Instant expiration) throws Exception {
        given(presignedRequest.url()).willReturn(URI.create("https://s3.example/upload").toURL());
        given(presignedRequest.expiration()).willReturn(expiration);
        given(presignedRequest.signedHeaders()).willReturn(Map.of("host", List.of("s3.example")));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);
    }

    @Test
    @DisplayName("Presigner 실패는 내부 원인을 숨긴 안전한 상태 예외로 변환한다")
    void wrapPresignerFailure() {
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willThrow(SdkClientException.builder().message("credential detail").build());

        assertThatThrownBy(() -> service.issueUploadUrl(new BookCoverUploadUrlRequest("image/jpeg", 1024L, 256L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("표지 이미지 업로드 URL 발급에 실패했습니다.");
    }
}
