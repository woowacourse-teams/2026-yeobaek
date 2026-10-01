package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberWithdrawalE2eTest extends E2eTest {

    @Test
    @DisplayName("회원 탈퇴 후 계정과 참여 및 작성 댓글을 더 이상 조회할 수 없다")
    void removeWithdrawnMemberData() {
        String leavingClubCommentContent = "탈퇴 회원의 모임 댓글";
        String leavingPublicRoomCommentContent = "탈퇴 회원의 공개방 댓글";
        long leavingMemberId = createMember("탈퇴할 회원");
        long observingMemberId = createMember("결과를 조회할 회원");
        long bookId = createBook("탈퇴 데이터 삭제 책");
        ClubFixture club = createClub(observingMemberId, bookId, "탈퇴 데이터 삭제 모임");
        joinClub(leavingMemberId, club.joinCode());
        ReadingIds clubReading = findClubReadingIds(observingMemberId, club.clubId());
        long publicRoomId = findPublicRoomId(observingMemberId);
        ReadingIds publicRoomReading = findPublicRoomReadingIds(observingMemberId, publicRoomId);

        // 탈퇴 회원이 독서 모임에 삭제 대상 댓글을 작성한다
        createClubComment(
                leavingMemberId, club.clubId(), clubReading.sentenceId(), leavingClubCommentContent);
        // 탈퇴 회원이 공개방에 삭제 대상 댓글을 작성한다
        createPublicRoomComment(
                leavingMemberId, publicRoomId, publicRoomReading.sentenceId(), leavingPublicRoomCommentContent);

        // 회원이 서비스에서 탈퇴한다
        memberRequest(leavingMemberId)
                .when()
                .delete("/api/members/me")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        // 탈퇴한 회원 ID로 보호된 기능을 이용할 수 없는지 확인한다
        memberRequest(leavingMemberId)
                .when()
                .get("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("MEMBER_NOT_FOUND"));

        // 독서 모임 참여자 목록에서 탈퇴 회원이 사라졌는지 조회한다
        memberRequest(observingMemberId)
                .when()
                .get("/api/clubs/{clubId}", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("members.memberId", not(hasItem(Math.toIntExact(leavingMemberId))));

        // 독서 모임에서 탈퇴 회원의 댓글이 사라졌는지 조회한다
        memberRequest(observingMemberId)
                .when()
                .post("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views",
                        club.clubId(), clubReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments", empty());

        // 공개방에서 탈퇴 회원의 댓글이 사라졌는지 조회한다
        memberRequest(observingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        publicRoomId, publicRoomReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments", empty());
    }

    @Test
    @DisplayName("회원 탈퇴 후에도 다른 회원의 참여와 댓글 및 공유 독서 공간은 유지된다")
    void preserveOtherMemberDataAndReadingSpaces() {
        String leavingClubCommentContent = "탈퇴 회원의 모임 댓글";
        String remainingClubCommentContent = "남은 회원의 모임 댓글";
        String leavingPublicRoomCommentContent = "탈퇴 회원의 공개방 댓글";
        String remainingPublicRoomCommentContent = "남은 회원의 공개방 댓글";
        String remainingMemberNickname = "남아 있는 회원";
        long leavingMemberId = createMember("탈퇴할 회원");
        long remainingMemberId = createMember(remainingMemberNickname);
        long bookId = createBook("탈퇴 데이터 보존 책");
        ClubFixture club = createClub(remainingMemberId, bookId, "탈퇴 후에도 남을 모임");
        joinClub(leavingMemberId, club.joinCode());
        ReadingIds clubReading = findClubReadingIds(remainingMemberId, club.clubId());
        long publicRoomId = findPublicRoomId(remainingMemberId);
        ReadingIds publicRoomReading = findPublicRoomReadingIds(remainingMemberId, publicRoomId);

        // 탈퇴 회원의 공개방 방문 기록을 준비한다
        memberRequest(leavingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
        // 남은 회원의 공개방 방문 기록을 준비한다
        memberRequest(remainingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        // 두 회원이 독서 모임에 각자의 댓글을 작성한다
        createClubComment(
                leavingMemberId, club.clubId(), clubReading.sentenceId(), leavingClubCommentContent);
        createClubComment(
                remainingMemberId, club.clubId(), clubReading.sentenceId(), remainingClubCommentContent);
        // 두 회원이 공개방에 각자의 댓글을 작성한다
        createPublicRoomComment(
                leavingMemberId, publicRoomId, publicRoomReading.sentenceId(), leavingPublicRoomCommentContent);
        createPublicRoomComment(
                remainingMemberId, publicRoomId, publicRoomReading.sentenceId(), remainingPublicRoomCommentContent);

        // 한 회원이 서비스에서 탈퇴한다
        memberRequest(leavingMemberId)
                .when()
                .delete("/api/members/me")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        // 독서 모임과 책 참조 및 남은 회원의 참여가 유지되는지 조회한다
        memberRequest(remainingMemberId)
                .when()
                .get("/api/clubs/{clubId}", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("clubId", equalTo(Math.toIntExact(club.clubId())))
                .body("book.bookId", equalTo(Math.toIntExact(bookId)))
                .body("members.memberId", hasItem(Math.toIntExact(remainingMemberId)))
                .body("members.find { it.memberId == " + Math.toIntExact(remainingMemberId) + " }.nickname",
                        equalTo(remainingMemberNickname));

        // 독서 모임에서 남은 회원의 댓글이 유지되는지 조회한다
        memberRequest(remainingMemberId)
                .when()
                .post("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views",
                        club.clubId(), clubReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments.memberId", hasItem(Math.toIntExact(remainingMemberId)))
                .body("comments.content", hasItem(remainingClubCommentContent));

        // 공개방과 책 참조가 유지되는지 조회한다
        memberRequest(remainingMemberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRoomId", equalTo(Math.toIntExact(publicRoomId)))
                .body("book.bookId", equalTo(Math.toIntExact(bookId)));

        // 남은 회원의 공개방 방문 기록이 유지되는지 조회한다
        memberRequest(remainingMemberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms.publicRoomId", hasItem(Math.toIntExact(publicRoomId)));

        // 공개방에서 남은 회원의 댓글이 유지되는지 조회한다
        memberRequest(remainingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        publicRoomId, publicRoomReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments.memberId", hasItem(Math.toIntExact(remainingMemberId)))
                .body("comments.content", hasItem(remainingPublicRoomCommentContent));
    }
}
