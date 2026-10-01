package yeobaek.backend.e2e;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import yeobaek.backend.support.DatabaseCleaner;
import yeobaek.backend.support.TestcontainersConfiguration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "admin.token=e2e-admin-token"
)
@Import(TestcontainersConfiguration.class)
abstract class E2eTest {

    protected static final String MEMBER_ID_HEADER = "X-Member-Id";

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String ADMIN_TOKEN = "e2e-admin-token";

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    protected RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .accept(JSON)
                .contentType(JSON)
                .log().ifValidationFails();
    }

    protected RequestSpecification memberRequest(long memberId) {
        return request().header(MEMBER_ID_HEADER, memberId);
    }

    protected long createMember(String nickname) {
        return request()
                .body(Map.of("nickname", nickname))
                .when()
                .post("/api/members")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .extract().jsonPath().getLong("memberId");
    }

    protected long createBook(String title) {
        return createBook(title, List.of(
                "첫 번째 문단입니다.",
                "두 번째 문단입니다.",
                "세 번째 문단입니다.",
                "네 번째 문단입니다."));
    }

    protected long createBook(String title, List<String> passageContents) {
        List<Map<String, Object>> passages = passageContents.stream()
                .map(this::passage)
                .toList();
        Map<String, Object> body = Map.of(
                "title", title,
                "authors", List.of(Map.of("name", "테스트 작가")),
                "chapters", List.of(Map.of("title", "테스트 목차", "passages", passages)));

        return request()
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

    protected ClubFixture createClub(long memberId, long bookId, String name) {
        ValidatableResponse response = memberRequest(memberId)
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

    protected void joinClub(long memberId, String joinCode) {
        memberRequest(memberId)
                .body(Map.of("joinCode", joinCode))
                .when()
                .post("/api/clubs/join")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    protected long findPublicRoomId(long memberId) {
        return memberRequest(memberId)
                .when()
                .get("/api/public-rooms")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().jsonPath().getLong("publicRooms[0].publicRoomId");
    }

    protected ReadingIds findClubReadingIds(long memberId, long clubId) {
        return readingIds(memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", 4)
                .when()
                .get("/api/clubs/{clubId}/passages", clubId)
                .then()
                .log().ifValidationFails()
                .statusCode(200));
    }

    protected ReadingIds findPublicRoomReadingIds(long memberId, long publicRoomId) {
        return readingIds(memberRequest(memberId)
                .queryParam("from", 1)
                .queryParam("to", 4)
                .when()
                .get("/api/public-rooms/{publicRoomId}/passages", publicRoomId)
                .then()
                .log().ifValidationFails()
                .statusCode(200));
    }

    protected long createClubComment(long memberId, long clubId, long sentenceId, String content) {
        return createComment(memberRequest(memberId)
                .body(Map.of("content", content))
                .when()
                .post("/api/clubs/{clubId}/sentences/{sentenceId}/comments", clubId, sentenceId), content);
    }

    protected long createPublicRoomComment(
            long memberId, long publicRoomId, long sentenceId, String content) {
        return createComment(memberRequest(memberId)
                .body(Map.of("content", content))
                .when()
                .post("/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments",
                        publicRoomId, sentenceId), content);
    }

    private Map<String, Object> passage(String content) {
        return Map.of("sentences", List.of(Map.of("content", content)));
    }

    private ReadingIds readingIds(ValidatableResponse response) {
        return new ReadingIds(
                response.extract().jsonPath().getLong("passages[1].passageId"),
                response.extract().jsonPath().getLong("passages[0].sentences[0].sentenceId"));
    }

    private long createComment(io.restassured.response.Response response, String content) {
        return response.then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("content", equalTo(content))
                .extract().jsonPath().getLong("commentId");
    }

    protected record ClubFixture(long clubId, String joinCode) {
    }

    protected record ReadingIds(long secondPassageId, long sentenceId) {
    }
}
