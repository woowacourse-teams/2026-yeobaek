package yeobaek.backend.book.service;

import static yeobaek.backend.support.LogField.ATTEMPT;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import yeobaek.backend.support.storage.S3StorageProperties;

@Component
@Slf4j
public class BookCoverUrlResolver {

    private final String publicBaseUrl;

    public BookCoverUrlResolver(S3StorageProperties properties) {
        this.publicBaseUrl = stripTrailingSlash(properties.publicBaseUrl());
    }

    public String resolve(String coverImageKey) {
        log.atInfo().addKeyValue(OPERATION, "bookCover.resolveUrl").addKeyValue(PHASE, ATTEMPT)
                .addKeyValue("coverPresent", coverImageKey != null).log("도서 표지 URL을 해석합니다.");
        if (coverImageKey == null) {
            log.atInfo().addKeyValue(OPERATION, "bookCover.resolveUrl").addKeyValue(PHASE, SUCCESS)
                    .addKeyValue("coverPresent", false).log("도서 표지 URL을 해석했습니다.");
            return null;
        }
        String resolved = publicBaseUrl + "/" + coverImageKey;
        log.atInfo().addKeyValue(OPERATION, "bookCover.resolveUrl").addKeyValue(PHASE, SUCCESS)
                .addKeyValue("coverPresent", true).log("도서 표지 URL을 해석했습니다.");
        return resolved;
    }

    private String stripTrailingSlash(String url) {
        int endIndex = url.length();
        while (endIndex > 0 && url.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return url.substring(0, endIndex);
    }
}
