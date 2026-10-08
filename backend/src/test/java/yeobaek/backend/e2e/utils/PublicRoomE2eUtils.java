package yeobaek.backend.e2e.utils;

import static yeobaek.backend.e2e.utils.E2eRequests.memberRequest;

public final class PublicRoomE2eUtils {

    private PublicRoomE2eUtils() {
    }

    public static long findPublicRoomId(int port, long memberId) {
        return memberRequest(port, memberId)
                .when()
                .get("/api/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().jsonPath().getLong("publicRooms[0].publicRoomId");
    }

    public static ReadingIds findPublicRoomReadingIds(int port, long memberId, long publicRoomId) {
        return ReadingIds.from(memberRequest(port, memberId)
                .queryParam("from", 1)
                .queryParam("to", 4)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200));
    }
}
