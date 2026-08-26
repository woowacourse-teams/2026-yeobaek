package yeobaek.backend.support.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3StoragePropertiesTest {

    @Test
    @DisplayName("버킷과 리전은 공백일 수 없다")
    void rejectBlankBucketAndRegion() {
        assertThatThrownBy(() -> new S3StorageProperties(" ", "ap-northeast-2", "https://cover.example"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties("bucket", " ", "https://cover.example"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공개 기준 URL은 절대 HTTP 또는 HTTPS URL이어야 한다")
    void rejectInvalidPublicBaseUrl() {
        assertThatThrownBy(() -> new S3StorageProperties("bucket", "ap-northeast-2", "/covers"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties("bucket", "ap-northeast-2", "ftp://cover.example"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
