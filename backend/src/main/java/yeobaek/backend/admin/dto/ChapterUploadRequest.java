package yeobaek.backend.admin.dto;

import java.util.List;

public record ChapterUploadRequest(
        String title,
        List<PassageUploadRequest> passages
) {

    public ChapterUploadRequest {
        passages = passages == null ? List.of() : List.copyOf(passages);
    }
}
