package watson.backend.admin.dto;

public record BookUploadResponse(
        Long bookId,
        String title,
        int passageCount
) {
}
