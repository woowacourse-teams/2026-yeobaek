package yeobaek.backend.club.domain.vo;

import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.PassageCount;

public record ProgressRate(double value) {

    private static final double MINIMUM_RATE = 0.0;
    private static final double MAXIMUM_RATE = 100.0;

    public static ProgressRate calculate(ContentSequence sequence, PassageCount count) {
        if (sequence.value() > count.value()) {
            throw new IllegalArgumentException("최근 읽은 본문 순서는 전체 본문 개수를 초과할 수 없습니다.");
        }
        return new ProgressRate(sequence.value() * MAXIMUM_RATE / count.value());
    }

    public ProgressRate {
        if (!Double.isFinite(value) || value < MINIMUM_RATE || value > MAXIMUM_RATE) {
            throw new IllegalArgumentException("진도율은 0 이상 100 이하여야 합니다.");
        }
    }

    public int roundedPercentage() {
        return (int) Math.round(value);
    }
}
