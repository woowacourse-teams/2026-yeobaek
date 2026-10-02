package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static yeobaek.backend.e2e.utils.BookE2eUtils.createBook;
import static yeobaek.backend.e2e.utils.ClubE2eUtils.createClub;
import static yeobaek.backend.e2e.utils.ClubE2eUtils.findClubReadingIds;
import static yeobaek.backend.e2e.utils.ClubE2eUtils.joinClub;
import static yeobaek.backend.e2e.utils.CommentE2eUtils.createClubComment;
import static yeobaek.backend.e2e.utils.CommentE2eUtils.createPublicRoomComment;
import static yeobaek.backend.e2e.utils.E2eRequests.memberRequest;
import static yeobaek.backend.e2e.utils.MemberE2eUtils.createMember;
import static yeobaek.backend.e2e.utils.PublicRoomE2eUtils.findPublicRoomId;
import static yeobaek.backend.e2e.utils.PublicRoomE2eUtils.findPublicRoomReadingIds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.e2e.utils.ClubE2eUtils.ClubFixture;
import yeobaek.backend.e2e.utils.ReadingIds;

class MemberWithdrawalE2eTest extends E2eTest {

    @Test
    @DisplayName("회원 탈퇴 후 계정과 참여 및 작성 댓글을 더 이상 조회할 수 없다")
    void removeWithdrawnMemberData() {
        String leavingClubCommentContent = "탈퇴 회원의 모임 댓글";
        String leavingPublicRoomCommentContent = "탈퇴 회원의 공개방 댓글";
        long leavingMemberId = createMember(port, "탈퇴할 회원");
        long observingMemberId = createMember(port, "결과를 조회할 회원");
        long bookId = createBook(port, "탈퇴 데이터 삭제 책");
        ClubFixture club = createClub(port, observingMemberId, bookId, "탈퇴 데이터 삭제 모임");
        joinClub(port, leavingMemberId, club.joinCode());
        ReadingIds clubReading = findClubReadingIds(port, observingMemberId, club.clubId());
        long publicRoomId = findPublicRoomId(port, observingMemberId);
        ReadingIds publicRoomReading = findPublicRoomReadingIds(port, observingMemberId, publicRoomId);

        // 탈퇴 회원이 독서 모임에 삭제 대상 댓글을 작성한다
        createClubComment(
                port, leavingMemberId, club.clubId(), clubReading.sentenceId(), leavingClubCommentContent);
        // 탈퇴 회원이 공개방에 삭제 대상 댓글을 작성한다
        createPublicRoomComment(
                port, leavingMemberId, publicRoomId,
                publicRoomReading.sentenceId(), leavingPublicRoomCommentContent);

        // 회원이 서비스에서 탈퇴한다
        memberRequest(port, leavingMemberId)
                .when()
                .delete("/api/members/me")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        // 탈퇴한 회원 ID로 보호된 기능을 이용할 수 없는지 확인한다
        memberRequest(port, leavingMemberId)
                .when()
                .get("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("MEMBER_NOT_FOUND"));

        // 독서 모임 참여자 목록에서 탈퇴 회원이 사라졌는지 조회한다
        memberRequest(port, observingMemberId)
                .when()
                .get("/api/clubs/{clubId}", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("members.memberId", not(hasItem(Math.toIntExact(leavingMemberId))));

        // 독서 모임에서 탈퇴 회원의 댓글이 사라졌는지 조회한다
        memberRequest(port, observingMemberId)
                .when()
                .post("/api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views",
                        club.clubId(), clubReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments", empty());

        // 공개방에서 탈퇴 회원의 댓글이 사라졌는지 조회한다
        memberRequest(port, observingMemberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views",
                        publicRoomId, publicRoomReading.sentenceId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("comments", empty());
    }

}
