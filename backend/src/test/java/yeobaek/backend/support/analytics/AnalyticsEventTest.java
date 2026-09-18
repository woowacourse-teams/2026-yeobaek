package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnalyticsEventTest {

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("events")
    @DisplayName("사용자 API 이벤트 이름과 속성을 생성한다")
    void createEvent(AnalyticsEvent actual, String name, Map<String, Object> properties) {
        assertThat(actual).isEqualTo(new AnalyticsEvent(name, properties));
    }

    private static Stream<Arguments> events() {
        return Stream.of(
                arguments(AnalyticsEvent.memberCreate(), "backend_member_create", Map.of()),
                arguments(AnalyticsEvent.memberDelete(), "backend_member_delete", Map.of()),
                arguments(AnalyticsEvent.blockedMembersView(2), "backend_blocked_members_view",
                        Map.of("blocked_member_count", 2)),
                arguments(AnalyticsEvent.memberBlock(), "backend_member_block", Map.of()),
                arguments(AnalyticsEvent.memberUnblock(), "backend_member_unblock", Map.of()),
                arguments(AnalyticsEvent.booksView(true, 3), "backend_books_view",
                        Map.of("search_used", true, "result_count", 3)),
                arguments(AnalyticsEvent.bookView(5L, 312, 4), "backend_book_view",
                        Map.of("book_id", 5L, "passage_count", 312, "chapter_count", 4)),
                arguments(AnalyticsEvent.clubCreate(10L, 5L), "backend_club_create",
                        Map.of("club_id", 10L, "book_id", 5L)),
                arguments(AnalyticsEvent.clubJoin(10L, 5L), "backend_club_join",
                        Map.of("club_id", 10L, "book_id", 5L)),
                arguments(AnalyticsEvent.clubLeave(10L), "backend_club_leave",
                        Map.of("club_id", 10L)),
                arguments(AnalyticsEvent.clubsView(2), "backend_clubs_view",
                        Map.of("club_count", 2)),
                arguments(AnalyticsEvent.clubView(10L, 5L, 4, 13, "ACTIVE"), "backend_club_view",
                        Map.of("club_id", 10L, "book_id", 5L, "member_count", 4,
                                "has_progress", true, "progress_rate", 13, "book_status", "ACTIVE")),
                arguments(AnalyticsEvent.clubView(10L, 5L, 4, null, "ACTIVE"), "backend_club_view",
                        Map.of("club_id", 10L, "book_id", 5L, "member_count", 4,
                                "has_progress", false, "book_status", "ACTIVE")),
                arguments(AnalyticsEvent.passagesView(10L, 42, 43, 2), "backend_passages_view",
                        Map.of("club_id", 10L, "from", 42, "to", 43, "passage_count", 2)),
                arguments(AnalyticsEvent.progressUpdate(10L, 1042L, 42, 13), "backend_progress_update",
                        Map.of("club_id", 10L, "passage_id", 1042L,
                                "last_read_passage_sequence", 42, "progress_rate", 13)),
                arguments(AnalyticsEvent.lastReadingView(), "backend_last_reading_view",
                        Map.of("has_last_reading", false)),
                arguments(AnalyticsEvent.lastReadingView(10L, 5L, 42, 13), "backend_last_reading_view",
                        Map.of("has_last_reading", true, "club_id", 10L, "book_id", 5L,
                                "last_read_passage_sequence", 42, "progress_rate", 13)),
                arguments(AnalyticsEvent.newCommentCountView(10L, 1042L, 3),
                        "backend_new_comment_count_view",
                        Map.of("club_id", 10L, "current_passage_id", 1042L, "new_comment_count", 3L)),
                arguments(AnalyticsEvent.commentedSentencesView(10L, 1042L, 2),
                        "backend_commented_sentences_view",
                        Map.of("club_id", 10L, "current_passage_id", 1042L,
                                "commented_sentence_count", 2)),
                arguments(AnalyticsEvent.commentsViewFromDeprecatedGet(10L, 5012L, 2),
                        "backend_comments_view",
                        Map.of("club_id", 10L, "sentence_id", 5012L, "comment_count", 2,
                                "api_variant", "deprecated_get")),
                arguments(AnalyticsEvent.commentsViewFromExplicitPost(10L, 5012L, 2),
                        "backend_comments_view",
                        Map.of("club_id", 10L, "sentence_id", 5012L, "comment_count", 2,
                                "api_variant", "explicit_post")),
                arguments(AnalyticsEvent.commentCreate(10L, 5012L, 7L), "backend_comment_create",
                        Map.of("club_id", 10L, "sentence_id", 5012L, "comment_id", 7L)),
                arguments(AnalyticsEvent.commentUpdate(7L), "backend_comment_update",
                        Map.of("comment_id", 7L)),
                arguments(AnalyticsEvent.commentDelete(7L), "backend_comment_delete",
                        Map.of("comment_id", 7L)),
                arguments(AnalyticsEvent.commentReport(7L), "backend_comment_report",
                        Map.of("comment_id", 7L))
        );
    }
}
