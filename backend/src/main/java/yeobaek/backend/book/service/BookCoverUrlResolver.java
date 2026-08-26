package yeobaek.backend.book.service;

import org.springframework.stereotype.Component;
import yeobaek.backend.support.storage.S3StorageProperties;

@Component
public class BookCoverUrlResolver {

    private final String publicBaseUrl;

    public BookCoverUrlResolver(S3StorageProperties properties) {
        this.publicBaseUrl = stripTrailingSlash(properties.publicBaseUrl());
    }

    public String resolve(String coverImageKey) {
        if (coverImageKey == null) {
            return null;
        }
        return publicBaseUrl + "/" + coverImageKey;
    }

    private static String stripTrailingSlash(String url) {
        int endIndex = url.length();
        while (endIndex > 0 && url.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return url.substring(0, endIndex);
    }
}
