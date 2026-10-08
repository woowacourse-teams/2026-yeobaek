package yeobaek.backend.book.service;

import org.springframework.stereotype.Component;
import yeobaek.backend.support.storage.S3StorageProperties;

@Component
public class BookCoverUrlResolver {

    private final String publicBaseUrl;
    private final String coverPrefix;

    public BookCoverUrlResolver(S3StorageProperties properties) {
        this.publicBaseUrl = stripTrailingSlash(properties.publicBaseUrl());
        this.coverPrefix = properties.prefix() + "/book-covers/";
    }

    public String resolve(String coverImageKey) {
        if (coverImageKey == null) {
            return null;
        }
        String key = coverImageKey.contains("/") ? coverImageKey : coverPrefix + coverImageKey;
        return publicBaseUrl + "/" + key;
    }

    private String stripTrailingSlash(String url) {
        int endIndex = url.length();
        while (endIndex > 0 && url.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return url.substring(0, endIndex);
    }
}
