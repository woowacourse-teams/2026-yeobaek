package yeobaek.backend.e2e.utils;

import static yeobaek.backend.e2e.utils.E2eRequests.request;

import java.util.Map;

public final class MemberE2eUtils {

    private MemberE2eUtils() {
    }

    public static long createMember(int port, String nickname) {
        return request(port)
                .body(Map.of("nickname", nickname))
                .when()
                .post("/api/members")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .extract().jsonPath().getLong("memberId");
    }
}
