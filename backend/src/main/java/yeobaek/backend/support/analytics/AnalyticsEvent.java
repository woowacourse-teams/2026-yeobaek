package yeobaek.backend.support.analytics;

import java.util.LinkedHashMap;
import java.util.Map;

public record AnalyticsEvent(
        String name,
        Map<String, Object> properties
) {

    private static final String MEMBER_CREATE = "backend_member_create";
    private static final String MEMBER_DELETE = "backend_member_delete";
    private static final String BLOCKED_MEMBERS_VIEW = "backend_blocked_members_view";
    private static final String MEMBER_BLOCK = "backend_member_block";
    private static final String MEMBER_UNBLOCK = "backend_member_unblock";
    private static final String BOOKS_VIEW = "backend_books_view";
    private static final String BOOK_VIEW = "backend_book_view";
    private static final String CLUB_CREATE = "backend_club_create";
    private static final String CLUB_JOIN = "backend_club_join";
    private static final String CLUB_LEAVE = "backend_club_leave";
    private static final String CLUBS_VIEW = "backend_clubs_view";
    private static final String CLUB_VIEW = "backend_club_view";
    private static final String PASSAGES_VIEW = "backend_passages_view";
    private static final String PROGRESS_UPDATE = "backend_progress_update";
    private static final String LAST_READING_VIEW = "backend_last_reading_view";
    private static final String NEW_COMMENT_COUNT_VIEW = "backend_new_comment_count_view";
    private static final String COMMENTED_SENTENCES_VIEW = "backend_commented_sentences_view";
    private static final String COMMENTS_VIEW = "backend_comments_view";
    private static final String COMMENT_CREATE = "backend_comment_create";
    private static final String COMMENT_UPDATE = "backend_comment_update";
    private static final String COMMENT_DELETE = "backend_comment_delete";
    private static final String COMMENT_REPORT = "backend_comment_report";

    private static final String CLUB_ID = "club_id";
    private static final String BOOK_ID = "book_id";
    private static final String PASSAGE_ID = "passage_id";
    private static final String SENTENCE_ID = "sentence_id";
    private static final String COMMENT_ID = "comment_id";
    private static final String CURRENT_PASSAGE_ID = "current_passage_id";
    private static final String PROGRESS_RATE = "progress_rate";
    private static final String LAST_READ_PASSAGE_SEQUENCE = "last_read_passage_sequence";
    private static final String API_VARIANT = "api_variant";
    private static final String DEPRECATED_GET = "deprecated_get";
    private static final String EXPLICIT_POST = "explicit_post";

    public AnalyticsEvent {
        properties = Map.copyOf(properties);
    }

    public static AnalyticsEvent memberCreate() {
        return event(MEMBER_CREATE);
    }

    public static AnalyticsEvent memberDelete() {
        return event(MEMBER_DELETE);
    }

    public static AnalyticsEvent blockedMembersView(int blockedMemberCount) {
        return event(BLOCKED_MEMBERS_VIEW, Map.of("blocked_member_count", blockedMemberCount));
    }

    public static AnalyticsEvent memberBlock() {
        return event(MEMBER_BLOCK);
    }

    public static AnalyticsEvent memberUnblock() {
        return event(MEMBER_UNBLOCK);
    }

    public static AnalyticsEvent booksView(boolean searchUsed, int resultCount) {
        return event(BOOKS_VIEW, Map.of(
                "search_used", searchUsed,
                "result_count", resultCount
        ));
    }

    public static AnalyticsEvent bookView(Long bookId, int passageCount, int chapterCount) {
        return event(BOOK_VIEW, Map.of(
                BOOK_ID, bookId,
                "passage_count", passageCount,
                "chapter_count", chapterCount
        ));
    }

    public static AnalyticsEvent clubCreate(Long clubId, Long bookId) {
        return clubAndBookEvent(CLUB_CREATE, clubId, bookId);
    }

    public static AnalyticsEvent clubJoin(Long clubId, Long bookId) {
        return clubAndBookEvent(CLUB_JOIN, clubId, bookId);
    }

    public static AnalyticsEvent clubLeave(Long clubId) {
        return event(CLUB_LEAVE, Map.of(CLUB_ID, clubId));
    }

    public static AnalyticsEvent clubsView(int clubCount) {
        return event(CLUBS_VIEW, Map.of("club_count", clubCount));
    }

    public static AnalyticsEvent clubView(Long clubId, Long bookId, int memberCount,
                                          Integer progressRate, String bookStatus) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(CLUB_ID, clubId);
        properties.put(BOOK_ID, bookId);
        properties.put("member_count", memberCount);
        properties.put("has_progress", progressRate != null);
        if (progressRate != null) {
            properties.put(PROGRESS_RATE, progressRate);
        }
        properties.put("book_status", bookStatus);
        return event(CLUB_VIEW, properties);
    }

    public static AnalyticsEvent passagesView(Long clubId, int from, int to, int passageCount) {
        return event(PASSAGES_VIEW, Map.of(
                CLUB_ID, clubId,
                "from", from,
                "to", to,
                "passage_count", passageCount
        ));
    }

    public static AnalyticsEvent progressUpdate(Long clubId, Long passageId,
                                                int lastReadPassageSequence, int progressRate) {
        return event(PROGRESS_UPDATE, Map.of(
                CLUB_ID, clubId,
                PASSAGE_ID, passageId,
                LAST_READ_PASSAGE_SEQUENCE, lastReadPassageSequence,
                PROGRESS_RATE, progressRate
        ));
    }

    public static AnalyticsEvent lastReadingView() {
        return event(LAST_READING_VIEW, Map.of("has_last_reading", false));
    }

    public static AnalyticsEvent lastReadingView(Long clubId, Long bookId,
                                                 int lastReadPassageSequence, int progressRate) {
        return event(LAST_READING_VIEW, Map.of(
                "has_last_reading", true,
                CLUB_ID, clubId,
                BOOK_ID, bookId,
                LAST_READ_PASSAGE_SEQUENCE, lastReadPassageSequence,
                PROGRESS_RATE, progressRate
        ));
    }

    public static AnalyticsEvent newCommentCountView(Long clubId, Long currentPassageId,
                                                     long newCommentCount) {
        return event(NEW_COMMENT_COUNT_VIEW, Map.of(
                CLUB_ID, clubId,
                CURRENT_PASSAGE_ID, currentPassageId,
                "new_comment_count", newCommentCount
        ));
    }

    public static AnalyticsEvent commentedSentencesView(Long clubId, Long currentPassageId,
                                                        int commentedSentenceCount) {
        return event(COMMENTED_SENTENCES_VIEW, Map.of(
                CLUB_ID, clubId,
                CURRENT_PASSAGE_ID, currentPassageId,
                "commented_sentence_count", commentedSentenceCount
        ));
    }

    public static AnalyticsEvent commentsViewFromDeprecatedGet(Long clubId, Long sentenceId,
                                                               int commentCount) {
        return commentsView(clubId, sentenceId, commentCount, DEPRECATED_GET);
    }

    public static AnalyticsEvent commentsViewFromExplicitPost(Long clubId, Long sentenceId,
                                                              int commentCount) {
        return commentsView(clubId, sentenceId, commentCount, EXPLICIT_POST);
    }

    public static AnalyticsEvent commentCreate(Long clubId, Long sentenceId, Long commentId) {
        return event(COMMENT_CREATE, Map.of(
                CLUB_ID, clubId,
                SENTENCE_ID, sentenceId,
                COMMENT_ID, commentId
        ));
    }

    public static AnalyticsEvent commentUpdate(Long commentId) {
        return commentEvent(COMMENT_UPDATE, commentId);
    }

    public static AnalyticsEvent commentDelete(Long commentId) {
        return commentEvent(COMMENT_DELETE, commentId);
    }

    public static AnalyticsEvent commentReport(Long commentId) {
        return commentEvent(COMMENT_REPORT, commentId);
    }

    private static AnalyticsEvent commentsView(Long clubId, Long sentenceId, int commentCount,
                                               String apiVariant) {
        return event(COMMENTS_VIEW, Map.of(
                CLUB_ID, clubId,
                SENTENCE_ID, sentenceId,
                "comment_count", commentCount,
                API_VARIANT, apiVariant
        ));
    }

    private static AnalyticsEvent clubAndBookEvent(String name, Long clubId, Long bookId) {
        return event(name, Map.of(
                CLUB_ID, clubId,
                BOOK_ID, bookId
        ));
    }

    private static AnalyticsEvent commentEvent(String name, Long commentId) {
        return event(name, Map.of(COMMENT_ID, commentId));
    }

    private static AnalyticsEvent event(String name) {
        return event(name, Map.of());
    }

    private static AnalyticsEvent event(String name, Map<String, Object> properties) {
        return new AnalyticsEvent(name, properties);
    }
}
