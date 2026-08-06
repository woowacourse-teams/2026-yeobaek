package watson.backend.admin.dto;

/**
 * 업로드 도서의 작가 항목. {name, isni?} 또는 {authorId} 중 한 형태만 허용한다 (API.md 6장).
 */
public record AuthorEntryRequest(
        Long authorId,
        String name,
        String isni
) {

    public boolean referencesExisting() {
        return authorId != null;
    }
}
