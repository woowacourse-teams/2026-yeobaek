package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.support.storage.S3StorageProperties;

class BookCoverUrlResolverTest {

    @Test
    @DisplayName("객체 키를 공개 기준 URL과 조합하고 키가 없으면 null을 반환한다")
    void resolvePublicUrl() {
        BookCoverUrlResolver resolver = new BookCoverUrlResolver(
                new S3StorageProperties("bucket", "ap-northeast-2", "https://cover.example///"));

        assertThat(resolver.resolve("book-covers/123e4567-e89b-12d3-a456-426614174000.jpg"))
                .isEqualTo("https://cover.example/book-covers/123e4567-e89b-12d3-a456-426614174000.jpg");
        assertThat(resolver.resolve(null)).isNull();
    }
}
