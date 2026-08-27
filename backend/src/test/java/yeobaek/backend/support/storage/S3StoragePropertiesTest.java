package yeobaek.backend.support.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3StoragePropertiesTest {

    @Test
    @DisplayName("버킷과 리전은 공백일 수 없다")
    void rejectBlankBucketAndRegion() {
        assertThatThrownBy(() -> new S3StorageProperties(" ", "ap-northeast-2", "https://cover.example", "yeobaek"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties("bucket", " ", "https://cover.example", "yeobaek"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공개 기준 URL은 절대 HTTP 또는 HTTPS URL이어야 한다")
    void rejectInvalidPublicBaseUrl() {
        assertThatThrownBy(() -> new S3StorageProperties("bucket", "ap-northeast-2", "/covers", "yeobaek"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties(
                "bucket", "ap-northeast-2", "ftp://cover.example", "yeobaek"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("객체 prefix는 중첩 경로로 설정할 수 있다")
    void configureNestedPrefix() {
        assertThat(new S3StorageProperties(
                "bucket", "ap-northeast-2", "https://cover.example", "team/yeobaek").prefix())
                .isEqualTo("team/yeobaek");
    }

    @Test
    @DisplayName("객체 prefix는 중복 슬래시 없이 1~26자여야 한다")
    void rejectInvalidPrefix() {
        assertThatThrownBy(() -> new S3StorageProperties(
                "bucket", "ap-northeast-2", "https://cover.example", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties(
                "bucket", "ap-northeast-2", "https://cover.example", "/yeobaek"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties(
                "bucket", "ap-northeast-2", "https://cover.example", "yeobaek/"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties(
                "bucket", "ap-northeast-2", "https://cover.example", "team//yeobaek"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new S3StorageProperties(
                "bucket", "ap-northeast-2", "https://cover.example", "a".repeat(27)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
