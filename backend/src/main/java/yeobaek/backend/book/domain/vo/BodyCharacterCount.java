package yeobaek.backend.book.domain.vo;

import jakarta.persistence.Embeddable;
import java.text.Normalizer;
import java.util.stream.Stream;

@Embeddable
public record BodyCharacterCount(long value) {

    private static final long CHARACTERS_PER_PAGE = 700;

    public BodyCharacterCount {
        if (value <= 0) {
            throw new IllegalArgumentException("본문 글자 수는 양수여야 합니다.");
        }
    }

    public static BodyCharacterCount fromParagraphs(Stream<String> paragraphs) {
        long total = paragraphs.mapToLong(BodyCharacterCount::countParagraph).reduce(0, Math::addExact);
        return new BodyCharacterCount(total);
    }

    private static long countParagraph(String rawParagraph) {
        String normalized = Normalizer.normalize(rawParagraph, Normalizer.Form.NFC);
        return normalized.codePoints().filter(codePoint -> codePoint != '\r' && codePoint != '\n' && codePoint != '\t')
                .count();
    }

    public long pageCount() {
        return value / CHARACTERS_PER_PAGE + (value % CHARACTERS_PER_PAGE == 0 ? 0 : 1);
    }
}
