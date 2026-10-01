package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicRoomE2eTest extends E2eTest {

    @Test
    @DisplayName("새로운 공개방에 처음 방문하고 책 본문을 읽는다")
    void visitNewPublicRoomAndRead() {
        String firstPassageContent = "첫 번째 문단입니다.";
        List<String> passageContents = List.of(
                firstPassageContent,
                "두 번째 문단입니다.",
                "세 번째 문단입니다.",
                "네 번째 문단입니다.");
        long memberId = createMember("첫 방문 독자");
        long bookId = createBook("첫 방문 공개방의 책", passageContents);

        // 처음 이용할 공개방을 전체 목록에서 선택한다
        long publicRoomId = memberRequest(memberId)
                .when()
                .get("/api/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms", hasSize(1))
                .body("publicRooms[0].book.bookId", equalTo(Math.toIntExact(bookId)))
                .body("publicRooms[0].myProgress", nullValue())
                .extract().jsonPath().getLong("publicRooms[0].publicRoomId");

        // 선택한 공개방에 처음 방문한다
        memberRequest(memberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        // 첫 방문한 공개방이 내 방문 목록에 추가됐는지 조회한다
        memberRequest(memberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms", hasSize(1))
                .body("publicRooms[0].publicRoomId", equalTo(Math.toIntExact(publicRoomId)));

        // 공개방을 통해 책 본문을 읽는다
        memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", passageContents.size())
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(passageContents.size()))
                .body("passages[0].sequence", equalTo(1))
                .body("passages[0].sentences[0].content", equalTo(firstPassageContent));
    }

    @Test
    @DisplayName("이미 방문한 공개방으로 다시 돌아와 책 본문을 읽는다")
    void returnToVisitedPublicRoomAndRead() {
        String firstPassageContent = "첫 번째 문단입니다.";
        List<String> passageContents = List.of(
                firstPassageContent,
                "두 번째 문단입니다.",
                "세 번째 문단입니다.",
                "네 번째 문단입니다.");
        long memberId = createMember("재방문 독자");
        createBook("재방문 공개방의 책", passageContents);
        long publicRoomId = findPublicRoomId(memberId);
        // 공개방을 이전에 방문한 상태로 준비한다
        visit(memberId, publicRoomId);

        // 내 방문 목록에서 이전에 이용한 공개방을 찾는다
        memberRequest(memberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms", hasSize(1))
                .body("publicRooms[0].publicRoomId", equalTo(Math.toIntExact(publicRoomId)))
                .body("publicRooms[0].lastVisitedAt", notNullValue());

        // 방문 목록에서 선택한 공개방에 다시 방문한다
        visit(memberId, publicRoomId);

        // 재방문한 공개방의 상세 정보를 조회한다
        memberRequest(memberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRoomId", equalTo(Math.toIntExact(publicRoomId)))
                .body("lastVisitedAt", notNullValue());

        // 재방문한 공개방을 통해 책 본문을 읽는다
        memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", 1)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(1))
                .body("passages[0].sentences[0].content", equalTo(firstPassageContent));
    }

    @Test
    @DisplayName("이미 이용한 공개방은 재방문 후에도 이전 진도율과 마지막 위치를 보존한다")
    void resumePublicRoomFromPreviousProgress() {
        String secondPassageContent = "두 번째 문단입니다.";
        List<String> passageContents = List.of(
                "첫 번째 문단입니다.",
                secondPassageContent,
                "세 번째 문단입니다.",
                "네 번째 문단입니다.");
        long memberId = createMember("공개방 이어 읽기 독자");
        createBook("이어 읽을 공개방의 책", passageContents);
        long publicRoomId = findPublicRoomId(memberId);
        // 이어 읽을 공개방에 방문한 상태로 준비한다
        visit(memberId, publicRoomId);
        ReadingIds reading = findPublicRoomReadingIds(memberId, publicRoomId);

        // 두 번째 문단까지 읽은 진도를 공개방에 저장한다
        memberRequest(memberId)
                .body(Map.of("passageId", reading.secondPassageId()))
                .when()
                .put("/api/public-rooms/{publicRoomId}/progress", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("lastReadPassageSequence", equalTo(2))
                .body("progressRate", equalTo(50));

        // 내 방문 목록에서 이전 진도율과 마지막 위치를 조회한다
        memberRequest(memberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms[0].myProgress.lastReadPassageSequence", equalTo(2))
                .body("publicRooms[0].myProgress.progressRate", equalTo(50));

        // 공개방 상세에서 이전 진도율과 마지막 위치를 조회한다
        memberRequest(memberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("myProgress.lastReadPassageSequence", equalTo(2))
                .body("myProgress.progressRate", equalTo(50));

        // 이전 진도가 있는 공개방에 다시 방문한다
        visit(memberId, publicRoomId);

        // 저장된 두 번째 문단부터 공개방 책 읽기를 이어간다
        memberRequest(memberId)
                .queryParam("from", 2)
                .queryParam("to", passageContents.size())
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(passageContents.size() - 1))
                .body("passages[0].sequence", equalTo(2))
                .body("passages[0].sentences[0].content", equalTo(secondPassageContent));

        // 재방문 뒤에도 이전 진도가 유지되는지 조회한다
        memberRequest(memberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("myProgress.lastReadPassageSequence", equalTo(2))
                .body("myProgress.progressRate", equalTo(50));
    }

    private void visit(long memberId, long publicRoomId) {
        // 회원의 공개방 방문 기록을 남긴다
        memberRequest(memberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));
    }
}
