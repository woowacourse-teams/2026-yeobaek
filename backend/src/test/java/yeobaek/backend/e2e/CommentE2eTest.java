package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.response.ValidatableResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CommentE2eTest extends E2eTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReadingSpaceType.class)
    @DisplayName("다른 회원의 댓글을 읽고 신고해도 댓글은 계속 보인다")
    void readAndReportAnotherMembersComment(ReadingSpaceType type) {
        CommentFixture fixture = createCommentFixture(type);
        // 작성자가 독서 공간의 문장에 신고 대상 댓글을 남긴다
        long commentId = createComment(fixture, fixture.writerId(), "신고해도 남을 댓글");

        // 독자가 다른 회원이 작성한 댓글을 읽는다
        viewComments(fixture.readerId(), fixture)
                .body("comments", hasSize(1))
                .body("comments[0].commentId", equalTo(Math.toIntExact(commentId)))
                .body("comments[0].memberId", equalTo(Math.toIntExact(fixture.writerId())))
                .body("comments[0].content", equalTo("신고해도 남을 댓글"))
                .body("comments[0].mine", equalTo(false));

        // 독자가 다른 회원의 댓글을 신고한다
        memberRequest(fixture.readerId())
                .when()
                .post("/api/comments/{commentId}/reports", commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        // 신고 뒤에도 댓글이 계속 보이는지 다시 조회한다
        viewComments(fixture.readerId(), fixture)
                .body("comments.commentId", hasItem(Math.toIntExact(commentId)))
                .body("comments[0].content", equalTo("신고해도 남을 댓글"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReadingSpaceType.class)
    @DisplayName("본인의 댓글을 작성하고 수정한 뒤 다시 조회한다")
    void createEditAndRequeryOwnComment(ReadingSpaceType type) {
        CommentFixture fixture = createCommentFixture(type);
        // 독자가 수정할 본인 댓글을 작성한다
        long commentId = createComment(fixture, fixture.readerId(), "수정 전 댓글");

        // 독자가 본인 댓글의 내용을 수정한다
        memberRequest(fixture.readerId())
                .body(Map.of("content", "수정한 댓글"))
                .when()
                .put("/api/comments/{commentId}", commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("commentId", equalTo(Math.toIntExact(commentId)))
                .body("content", equalTo("수정한 댓글"))
                .body("mine", equalTo(true));

        // 댓글 상세에서 수정된 내용을 다시 조회한다
        viewComments(fixture.readerId(), fixture)
                .body("comments", hasSize(1))
                .body("comments[0].commentId", equalTo(Math.toIntExact(commentId)))
                .body("comments[0].content", equalTo("수정한 댓글"))
                .body("comments[0].mine", equalTo(true));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReadingSpaceType.class)
    @DisplayName("본인의 댓글을 삭제하면 재조회와 본문의 댓글 수에 반영된다")
    void deleteOwnCommentAndRequeryCount(ReadingSpaceType type) {
        CommentFixture fixture = createCommentFixture(type);
        // 독자가 삭제할 본인 댓글을 작성한다
        long commentId = createComment(fixture, fixture.readerId(), "삭제할 댓글");

        // 댓글 작성이 본문의 댓글 수에 반영됐는지 조회한다
        viewPassages(fixture.readerId(), fixture)
                .body("passages[0].sentences[0].commentCount", equalTo(1));

        // 독자가 본인 댓글을 삭제한다
        memberRequest(fixture.readerId())
                .when()
                .delete("/api/comments/{commentId}", commentId)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        // 댓글 상세에서 삭제된 댓글이 사라졌는지 조회한다
        viewComments(fixture.readerId(), fixture)
                .body("comments", empty());

        // 댓글 삭제가 본문의 댓글 수에 반영됐는지 조회한다
        viewPassages(fixture.readerId(), fixture)
                .body("passages[0].sentences[0].commentCount", equalTo(0));
    }

    private CommentFixture createCommentFixture(ReadingSpaceType type) {
        long writerId = createMember(type + " 댓글 작성자");
        long readerId = createMember(type + " 댓글 독자");
        long bookId = createBook(type + " 댓글 책");

        if (type == ReadingSpaceType.CLUB) {
            ClubFixture club = createClub(writerId, bookId, "댓글 독서 모임");
            joinClub(readerId, club.joinCode());
            ReadingIds reading = findClubReadingIds(readerId, club.clubId());
            return new CommentFixture(type, club.clubId(), reading.sentenceId(), writerId, readerId);
        }

        long publicRoomId = findPublicRoomId(readerId);
        ReadingIds reading = findPublicRoomReadingIds(readerId, publicRoomId);
        return new CommentFixture(type, publicRoomId, reading.sentenceId(), writerId, readerId);
    }

    private long createComment(CommentFixture fixture, long memberId, String content) {
        if (fixture.type() == ReadingSpaceType.CLUB) {
            // 독서 모임의 문장에 댓글을 작성한다
            return createClubComment(memberId, fixture.spaceId(), fixture.sentenceId(), content);
        }
        // 공개방의 문장에 댓글을 작성한다
        return createPublicRoomComment(memberId, fixture.spaceId(), fixture.sentenceId(), content);
    }

    private ValidatableResponse viewComments(long memberId, CommentFixture fixture) {
        if (fixture.type() == ReadingSpaceType.CLUB) {
            // 독서 모임에서 문장의 댓글 상세를 조회한다
            return memberRequest(memberId)
                    .when()
                    .post("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views",
                            fixture.spaceId(), fixture.sentenceId())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);
        }
        // 공개방에서 문장의 댓글 상세를 조회한다
        return memberRequest(memberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        fixture.spaceId(), fixture.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    private ValidatableResponse viewPassages(long memberId, CommentFixture fixture) {
        if (fixture.type() == ReadingSpaceType.CLUB) {
            // 독서 모임 본문에서 문장의 댓글 수를 조회한다
            return memberRequest(memberId)
                    .queryParam("from", 1)
                    .queryParam("to", 1)
                    .when()
                    .get("/api/clubs/{clubId}/passages", fixture.spaceId())
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);
        }
        // 공개방 본문에서 문장의 댓글 수를 조회한다
        return memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", 1)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", fixture.spaceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    private enum ReadingSpaceType {
        CLUB,
        PUBLIC_ROOM
    }

    private record CommentFixture(
            ReadingSpaceType type,
            long spaceId,
            long sentenceId,
            long writerId,
            long readerId
    ) {
    }
}
