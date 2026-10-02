package yeobaek.backend.e2e.utils;

import io.restassured.response.ValidatableResponse;

public record ReadingIds(long secondPassageId, long sentenceId) {

    static ReadingIds from(ValidatableResponse response) {
        return new ReadingIds(
                response.extract().jsonPath().getLong("passages[1].passageId"),
                response.extract().jsonPath().getLong("passages[0].sentences[0].sentenceId"));
    }
}
