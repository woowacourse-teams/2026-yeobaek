package yeobaek.backend.e2e.utils;

import static org.hamcrest.Matchers.equalTo;
import static yeobaek.backend.e2e.utils.E2eRequests.request;

import java.util.List;
import java.util.Map;

public final class BookE2eUtils {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String ADMIN_TOKEN = "e2e-admin-token";

    private BookE2eUtils() {
    }

    public static long createBook(int port, String title) {
        return createBook(port, title, List.of(
                "첫 번째 문단입니다.",
                "두 번째 문단입니다.",
                "세 번째 문단입니다.",
                "네 번째 문단입니다."));
    }

    public static long createBook(int port, String title, List<String> passageContents) {
        List<Map<String, Object>> passages = passageContents.stream()
                .map(BookE2eUtils::passage)
                .toList();
        Map<String, Object> body = Map.of(
                "title", title,
                "authors", List.of(Map.of("name", "테스트 작가")),
                "chapters", List.of(Map.of("title", "테스트 목차", "passages", passages)));

        return request(port)
                .header(ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                .body(body)
                .when()
                .post("/api/admin/books")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("title", equalTo(title))
                .body("passageCount", equalTo(passageContents.size()))
                .extract().jsonPath().getLong("bookId");
    }

    private static Map<String, Object> passage(String content) {
        return Map.of("sentences", List.of(Map.of("content", content)));
    }
}
