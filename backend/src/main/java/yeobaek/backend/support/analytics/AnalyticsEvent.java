package yeobaek.backend.support.analytics;

import java.util.Map;

public record AnalyticsEvent(
        String name,
        Map<String, Object> properties
) {

    private static final String MEMBER_CREATED = "backend_member_created";
    private static final String CLUB_CREATED = "backend_club_created";
    private static final String CLUB_JOINED = "backend_club_joined";
    private static final String PASSAGES_VIEWED = "backend_passages_viewed";
    private static final String COMMENTS_VIEWED = "backend_comments_viewed";
    private static final String COMMENT_CREATED = "backend_comment_created";
    private static final String CLUB_ID = "club_id";
    private static final String BOOK_ID = "book_id";
    private static final String SENTENCE_ID = "sentence_id";

    public AnalyticsEvent {
        properties = Map.copyOf(properties);
    }

    public static AnalyticsEvent memberCreated() {
        return new AnalyticsEvent(MEMBER_CREATED, Map.of());
    }

    public static AnalyticsEvent clubCreated(Long clubId, Long bookId) {
        return new AnalyticsEvent(CLUB_CREATED, Map.of(
                CLUB_ID, clubId,
                BOOK_ID, bookId
        ));
    }

    public static AnalyticsEvent clubJoined(Long clubId, Long bookId) {
        return new AnalyticsEvent(CLUB_JOINED, Map.of(
                CLUB_ID, clubId,
                BOOK_ID, bookId
        ));
    }

    public static AnalyticsEvent passagesViewed(Long clubId, int from, int to, int passageCount) {
        return new AnalyticsEvent(PASSAGES_VIEWED, Map.of(
                CLUB_ID, clubId,
                "from", from,
                "to", to,
                "passage_count", passageCount
        ));
    }

    public static AnalyticsEvent commentsViewed(Long clubId, Long sentenceId, int commentCount) {
        return new AnalyticsEvent(COMMENTS_VIEWED, Map.of(
                CLUB_ID, clubId,
                SENTENCE_ID, sentenceId,
                "comment_count", commentCount
        ));
    }

    public static AnalyticsEvent commentCreated(Long clubId, Long sentenceId, Long commentId) {
        return new AnalyticsEvent(COMMENT_CREATED, Map.of(
                CLUB_ID, clubId,
                SENTENCE_ID, sentenceId,
                "comment_id", commentId
        ));
    }
}
