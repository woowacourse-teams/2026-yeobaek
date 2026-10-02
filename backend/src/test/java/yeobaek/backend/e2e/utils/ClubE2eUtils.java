package yeobaek.backend.e2e.utils;

import static yeobaek.backend.e2e.utils.E2eRequests.memberRequest;

import io.restassured.response.ValidatableResponse;
import java.util.Map;

public final class ClubE2eUtils {

    private ClubE2eUtils() {
    }

    public static ClubFixture createClub(int port, long memberId, long bookId, String name) {
        ValidatableResponse response = memberRequest(port, memberId)
                .body(Map.of("name", name, "bookId", bookId))
                .when()
                .post("/api/clubs")
                .then()
                .log().ifValidationFails()
                .statusCode(201);

        return new ClubFixture(
                response.extract().jsonPath().getLong("clubId"),
                response.extract().jsonPath().getString("joinCode"));
    }

    public static void joinClub(int port, long memberId, String joinCode) {
        memberRequest(port, memberId)
                .body(Map.of("joinCode", joinCode))
                .when()
                .post("/api/clubs/join")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    public static ReadingIds findClubReadingIds(int port, long memberId, long clubId) {
        return ReadingIds.from(memberRequest(port, memberId)
                .queryParam("from", 1)
                .queryParam("to", 4)
                .when()
                .get("/api/clubs/{clubId}/passages", clubId)
                .then()
                .log().ifValidationFails()
                .statusCode(200));
    }

    public record ClubFixture(long clubId, String joinCode) {
    }
}
