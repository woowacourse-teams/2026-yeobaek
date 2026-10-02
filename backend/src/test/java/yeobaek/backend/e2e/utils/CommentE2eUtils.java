package yeobaek.backend.e2e.utils;

import static org.hamcrest.Matchers.equalTo;
import static yeobaek.backend.e2e.utils.E2eRequests.memberRequest;

import io.restassured.response.Response;
import java.util.Map;

public final class CommentE2eUtils {

    private CommentE2eUtils() {
    }

    public static long createClubComment(
            int port, long memberId, long clubId, long sentenceId, String content) {
        return createComment(memberRequest(port, memberId)
                .body(Map.of("content", content))
                .when()
                .post("/api/clubs/{clubId}/sentences/{sentenceId}/comments", clubId, sentenceId), content);
    }

    public static long createPublicRoomComment(
            int port, long memberId, long publicRoomId, long sentenceId, String content) {
        return createComment(memberRequest(port, memberId)
                .body(Map.of("content", content))
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments",
                        publicRoomId, sentenceId), content);
    }

    private static long createComment(Response response, String content) {
        return response.then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("content", equalTo(content))
                .extract().jsonPath().getLong("commentId");
    }
}
