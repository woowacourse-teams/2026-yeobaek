package yeobaek.backend.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("잘못된 요청의 상태, 코드, 메시지를 변환한다")
    void handleIllegalArgument() {
        var exception = new IllegalArgumentException("요청 값 검증 실패 원문");

        var response = handler.handleIllegalArgument(exception);
        var body = response.getBody();

        assertNotNull(body, "예외 변환 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST, response.getStatusCode(), "잘못된 요청은 400으로 변환해야 한다"),
                () -> assertEquals("INVALID_REQUEST", body.code(), "잘못된 요청의 코드를 반환해야 한다"),
                () -> assertEquals(
                        "요청 값 검증 실패 원문", body.message(), "예외 메시지를 변경하지 않고 전달해야 한다"));
    }

    @Test
    @DisplayName("이용할 수 없는 도서 예외를 400과 개별 코드로 변환한다")
    void handleBookNotAvailable() {
        var response = handler.handleBadRequest(new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE));
        var body = response.getBody();

        assertNotNull(body, "예외 변환 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST, response.getStatusCode(), "이용 불가 도서는 400으로 변환해야 한다"),
                () -> assertEquals(
                        "BOOK_NOT_AVAILABLE", body.code(), "이용 불가 도서의 개별 코드를 반환해야 한다"),
                () -> assertEquals(
                        "더 이상 이용할 수 없는 도서입니다.", body.message(), "이용 불가 안내 메시지를 반환해야 한다"));
    }
}
