package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberWithdrawalE2eTest extends E2eTest {

    @Test
    @DisplayName("회원 탈퇴 후 외부에서 관찰 가능한 회원 데이터는 사라지고 공유 공간은 유지된다")
    void withdrawMember() {
        long leavingMemberId = createMember("탈퇴할 회원");
        long remainingMemberId = createMember("남아 있는 회원");
        long bookId = createBook("탈퇴 시나리오의 책");
        ClubFixture club = createClub(remainingMemberId, bookId, "탈퇴 후에도 남을 모임");
        joinClub(leavingMemberId, club.joinCode());
        ReadingIds clubReading = findClubReadingIds(remainingMemberId, club.clubId());
        long publicRoomId = findPublicRoomId(remainingMemberId);
        ReadingIds publicRoomReading = findPublicRoomReadingIds(remainingMemberId, publicRoomId);

        memberRequest(leavingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
        memberRequest(remainingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);

        createClubComment(
                leavingMemberId, club.clubId(), clubReading.sentenceId(), "탈퇴 회원의 모임 댓글");
        createClubComment(
                remainingMemberId, club.clubId(), clubReading.sentenceId(), "남은 회원의 모임 댓글");
        createPublicRoomComment(
                leavingMemberId, publicRoomId, publicRoomReading.sentenceId(), "탈퇴 회원의 공개방 댓글");
        createPublicRoomComment(
                remainingMemberId, publicRoomId, publicRoomReading.sentenceId(), "남은 회원의 공개방 댓글");

        memberRequest(leavingMemberId)
                .when()
                .delete("/api/members/me")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        memberRequest(leavingMemberId)
                .when()
                .get("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("MEMBER_NOT_FOUND"));

        memberRequest(remainingMemberId)
                .when()
                .get("/api/clubs/{clubId}", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("clubId", equalTo(Math.toIntExact(club.clubId())))
                .body("members", hasSize(1))
                .body("members[0].memberId", equalTo(Math.toIntExact(remainingMemberId)))
                .body("members[0].nickname", equalTo("남아 있는 회원"));

        memberRequest(remainingMemberId)
                .when()
                .post("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views",
                        club.clubId(), clubReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments", hasSize(1))
                .body("comments[0].memberId", equalTo(Math.toIntExact(remainingMemberId)))
                .body("comments[0].content", equalTo("남은 회원의 모임 댓글"));

        memberRequest(remainingMemberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRoomId", equalTo(Math.toIntExact(publicRoomId)))
                .body("book.bookId", equalTo(Math.toIntExact(bookId)));

        memberRequest(remainingMemberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms", hasSize(1))
                .body("publicRooms[0].publicRoomId", equalTo(Math.toIntExact(publicRoomId)));

        memberRequest(remainingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        publicRoomId, publicRoomReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments", hasSize(1))
                .body("comments[0].memberId", equalTo(Math.toIntExact(remainingMemberId)))
                .body("comments[0].content", equalTo("남은 회원의 공개방 댓글"));
    }
}
