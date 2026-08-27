package yeobaek.backend.support.storage;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        String publicBaseUrl,
        String prefix
) {

    private static final int MAX_PREFIX_LENGTH = 26;

    public S3StorageProperties {
        requireNonBlank(bucket, "storage.s3.bucket");
        requireNonBlank(region, "storage.s3.region");
        requirePublicHttpUrl(publicBaseUrl);
        requireValidPrefix(prefix);
    }

    private static void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " 설정은 필수입니다.");
        }
    }

    private static void requirePublicHttpUrl(String value) {
        requireNonBlank(value, "storage.s3.public-base-url");
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("storage.s3.public-base-url은 올바른 절대 URL이어야 합니다.",
                    exception);
        }
        if (!uri.isAbsolute()
                || uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("storage.s3.public-base-url은 절대 http(s) URL이어야 합니다.");
        }
    }

    private static void requireValidPrefix(String value) {
        requireNonBlank(value, "storage.s3.prefix");
        if (value.length() > MAX_PREFIX_LENGTH
                || value.startsWith("/")
                || value.endsWith("/")
                || value.contains("//")) {
            throw new IllegalArgumentException(
                    "storage.s3.prefix는 슬래시로 시작하거나 끝나지 않는 1~" + MAX_PREFIX_LENGTH + "자 경로여야 합니다.");
        }
    }
}
