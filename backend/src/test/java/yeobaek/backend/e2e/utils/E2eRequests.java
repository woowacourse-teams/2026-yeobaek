package yeobaek.backend.e2e.utils;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

import io.restassured.specification.RequestSpecification;

public final class E2eRequests {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private E2eRequests() {
    }

    static RequestSpecification request(int port) {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .accept(JSON)
                .contentType(JSON)
                .log().ifValidationFails();
    }

    public static RequestSpecification memberRequest(int port, long memberId) {
        return request(port).header(MEMBER_ID_HEADER, memberId);
    }
}
