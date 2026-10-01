package yeobaek.backend.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicRoomE2eTest extends E2eTest {

    @Test
    @DisplayName("새로운 공개방에 처음 방문하고 책 본문을 읽는다")
    void visitNewPublicRoomAndRead() {
        long memberId = createMember("첫 방문 독자");
        long bookId = createBook("첫 방문 공개방의 책");

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

        memberRequest(memberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));

        memberRequest(memberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms", hasSize(1))
                .body("publicRooms[0].publicRoomId", equalTo(Math.toIntExact(publicRoomId)));

        memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", 4)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(4))
                .body("passages[0].sequence", equalTo(1))
                .body("passages[0].sentences[0].content", equalTo("첫 번째 문단입니다."));
    }

    @Test
    @DisplayName("이미 방문한 공개방으로 다시 돌아와 책 본문을 읽는다")
    void returnToVisitedPublicRoomAndRead() {
        long memberId = createMember("재방문 독자");
        createBook("재방문 공개방의 책");
        long publicRoomId = findPublicRoomId(memberId);
        visit(memberId, publicRoomId);

        memberRequest(memberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms", hasSize(1))
                .body("publicRooms[0].publicRoomId", equalTo(Math.toIntExact(publicRoomId)))
                .body("publicRooms[0].lastVisitedAt", notNullValue());

        visit(memberId, publicRoomId);

        memberRequest(memberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRoomId", equalTo(Math.toIntExact(publicRoomId)))
                .body("lastVisitedAt", notNullValue());

        memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", 1)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(1))
                .body("passages[0].sentences[0].content", equalTo("첫 번째 문단입니다."));
    }

    @Test
    @DisplayName("이미 이용한 공개방은 재방문 후에도 이전 진도율과 마지막 위치를 보존한다")
    void resumePublicRoomFromPreviousProgress() {
        long memberId = createMember("공개방 이어 읽기 독자");
        createBook("이어 읽을 공개방의 책");
        long publicRoomId = findPublicRoomId(memberId);
        visit(memberId, publicRoomId);
        ReadingIds reading = findPublicRoomReadingIds(memberId, publicRoomId);

        memberRequest(memberId)
                .body(Map.of("passageId", reading.secondPassageId()))
                .when()
                .put("/api/public-rooms/{publicRoomId}/progress", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("lastReadPassageSequence", equalTo(2))
                .body("progressRate", equalTo(50));

        memberRequest(memberId)
                .when()
                .get("/api/members/me/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("publicRooms[0].myProgress.lastReadPassageSequence", equalTo(2))
                .body("publicRooms[0].myProgress.progressRate", equalTo(50));

        memberRequest(memberId)
                .when()
                .get("/api/public-rooms/{publicRoomId}", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("myProgress.lastReadPassageSequence", equalTo(2))
                .body("myProgress.progressRate", equalTo(50));

        visit(memberId, publicRoomId);

        memberRequest(memberId)
                .queryParam("from", 2)
                .queryParam("to", 4)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("passages", hasSize(3))
                .body("passages[0].sequence", equalTo(2))
                .body("passages[0].sentences[0].content", equalTo("두 번째 문단입니다."));

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
        memberRequest(memberId)
                .when()
                .post("/api/public-rooms/{publicRoomId}/visits", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(equalTo(""));
    }
}
