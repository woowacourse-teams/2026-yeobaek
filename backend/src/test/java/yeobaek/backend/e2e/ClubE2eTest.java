package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static yeobaek.backend.e2e.utils.BookE2eUtils.createBook;
import static yeobaek.backend.e2e.utils.ClubE2eUtils.createClub;
import static yeobaek.backend.e2e.utils.ClubE2eUtils.findClubReadingIds;
import static yeobaek.backend.e2e.utils.E2eRequests.memberRequest;
import static yeobaek.backend.e2e.utils.MemberE2eUtils.createMember;

import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.e2e.utils.ClubE2eUtils.ClubFixture;
import yeobaek.backend.e2e.utils.ReadingIds;

class ClubE2eTest extends E2eTest {

    @Test
    @DisplayName("참여 코드로 기존 독서 모임에 참여하고 책 본문을 읽는다")
    void joinExistingClubAndRead() {
        String firstPassageContent = "첫 번째 문단입니다.";
        List<String> passageContents = List.of(
                firstPassageContent,
                "두 번째 문단입니다.",
                "세 번째 문단입니다.",
                "네 번째 문단입니다.");
        long ownerId = createMember(port, "모임장");
        long readerId = createMember(port, "참여 독자");
        long bookId = createBook(port, "참여할 모임의 책", passageContents);
        ClubFixture club = createClub(port, ownerId, bookId, "기존 독서 모임");

        // 참여 독자가 기존 독서 모임에 들어간다
        memberRequest(port, readerId)
                .body(Map.of("joinCode", club.joinCode()))
                .when()
                .post("/api/clubs/join")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("clubId", equalTo(Math.toIntExact(club.clubId())))
                .body("name", equalTo("기존 독서 모임"))
                .body("book.bookId", equalTo(Math.toIntExact(bookId)));

        // 참여한 독서 모임이 내 모임 목록에 반영됐는지 조회한다
        memberRequest(port, readerId)
                .when()
                .get("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("clubs", hasSize(1))
                .body("clubs[0].clubId", equalTo(Math.toIntExact(club.clubId())))
                .body("clubs[0].memberCount", equalTo(2));

        // 독서 모임 상세에서 참여자 수를 확인한다
        memberRequest(port, readerId)
                .when()
                .get("/api/clubs/{clubId}", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("members", hasSize(2));

        // 참여한 독서 모임을 통해 책 본문을 읽는다
        memberRequest(port, readerId)
                .queryParam("from", 1)
                .queryParam("to", passageContents.size())
                .when()
                .get("/api/clubs/{clubId}/passages", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(passageContents.size()))
                .body("passages[0].sequence", equalTo(1))
                .body("passages[0].sentences[0].content", equalTo(firstPassageContent));
    }

    @Test
    @DisplayName("도서 목록에서 책을 선택해 새 독서 모임을 만들고 책 본문을 읽는다")
    void chooseBookCreateClubAndRead() {
        String fourthPassageContent = "네 번째 문단입니다.";
        List<String> passageContents = List.of(
                "첫 번째 문단입니다.",
                "두 번째 문단입니다.",
                "세 번째 문단입니다.",
                fourthPassageContent);
        long memberId = createMember(port, "새 모임 독자");
        long bookId = createBook(port, "새 모임의 책", passageContents);

        // 새 모임에서 읽을 책을 도서 목록에서 조회한다
        memberRequest(port, memberId)
                .when()
                .get("/api/books")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("books", hasSize(1))
                .body("books[0].bookId", equalTo(Math.toIntExact(bookId)))
                .body("books[0].title", equalTo("새 모임의 책"))
                .body("books[0].passageCount", equalTo(passageContents.size()));

        // 선택한 책으로 새 독서 모임을 생성한다
        ValidatableResponse createdClub = memberRequest(port, memberId)
                .body(Map.of("name", "새 독서 모임", "bookId", bookId))
                .when()
                .post("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("name", equalTo("새 독서 모임"))
                .body("joinCode", notNullValue())
                .body("book.bookId", equalTo(Math.toIntExact(bookId)));
        long clubId = createdClub.extract().jsonPath().getLong("clubId");

        // 생성한 독서 모임이 내 모임 목록에 반영됐는지 조회한다
        memberRequest(port, memberId)
                .when()
                .get("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("clubs", hasSize(1))
                .body("clubs[0].clubId", equalTo(Math.toIntExact(clubId)))
                .body("clubs[0].memberCount", equalTo(1));

        // 생성자가 새 독서 모임의 참여자로 등록됐는지 조회한다
        memberRequest(port, memberId)
                .when()
                .get("/api/clubs/{clubId}", clubId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("members", hasSize(1))
                .body("members[0].memberId", equalTo(Math.toIntExact(memberId)))
                .body("members[0].mine", equalTo(true));

        // 새 독서 모임을 통해 선택한 책의 본문을 읽는다
        memberRequest(port, memberId)
                .queryParam("from", 1)
                .queryParam("to", passageContents.size())
                .when()
                .get("/api/clubs/{clubId}/passages", clubId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(passageContents.size()))
                .body("passages[3].sequence", equalTo(4))
                .body("passages[3].sentences[0].content", equalTo(fourthPassageContent));
    }

    @Test
    @DisplayName("이미 참여한 독서 모임은 이전 진도율과 마지막 위치에서 이어 읽는다")
    void resumeParticipatedClubFromPreviousProgress() {
        String secondPassageContent = "두 번째 문단입니다.";
        List<String> passageContents = List.of(
                "첫 번째 문단입니다.",
                secondPassageContent,
                "세 번째 문단입니다.",
                "네 번째 문단입니다.");
        long memberId = createMember(port, "이어 읽는 독자");
        long bookId = createBook(port, "이어 읽을 모임의 책", passageContents);
        ClubFixture club = createClub(port, memberId, bookId, "이어 읽기 모임");
        ReadingIds reading = findClubReadingIds(port, memberId, club.clubId());

        // 두 번째 문단까지 읽은 진도를 독서 모임에 저장한다
        memberRequest(port, memberId)
                .body(Map.of("passageId", reading.secondPassageId()))
                .when()
                .put("/api/clubs/{clubId}/progress", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("lastReadPassageSequence", equalTo(2))
                .body("progressRate", equalTo(50));

        // 내 모임 목록에서 이전 진도율과 마지막 위치를 조회한다
        memberRequest(port, memberId)
                .when()
                .get("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("clubs", hasSize(1))
                .body("clubs[0].clubId", equalTo(Math.toIntExact(club.clubId())))
                .body("clubs[0].myProgress.lastReadPassageSequence", equalTo(2))
                .body("clubs[0].myProgress.progressRate", equalTo(50));

        // 독서 모임 상세에서 이전 진도율과 마지막 위치를 조회한다
        memberRequest(port, memberId)
                .when()
                .get("/api/clubs/{clubId}", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("myProgress.lastReadPassageSequence", equalTo(2))
                .body("myProgress.progressRate", equalTo(50));

        // 저장된 두 번째 문단부터 책 읽기를 이어간다
        memberRequest(port, memberId)
                .queryParam("from", 2)
                .queryParam("to", passageContents.size())
                .when()
                .get("/api/clubs/{clubId}/passages", club.clubId())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(passageContents.size() - 1))
                .body("passages[0].sequence", equalTo(2))
                .body("passages[0].sentences[0].content", equalTo(secondPassageContent));
    }
}
