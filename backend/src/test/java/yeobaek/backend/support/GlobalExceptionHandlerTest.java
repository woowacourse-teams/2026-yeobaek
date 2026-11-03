package yeobaek.backend.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("요청 DTO 검증 실패를 400과 INVALID_REQUEST로 변환한다")
    void handleValidation() {
        var response = handler.handleValidation();
        var body = response.getBody();

        assertNotNull(body, "검증 실패 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "검증 실패는 400이어야 한다"),
                () -> assertEquals("INVALID_REQUEST", body.code(), "기존 잘못된 요청 코드를 반환해야 한다"),
                () -> assertEquals("필수 요청 값이 누락되었습니다.", body.message(), "필수값 누락 안내를 반환해야 한다"));
    }

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
        var response = handler.handleBadRequest(new BadRequestException(
                ErrorCode.BOOK_NOT_AVAILABLE,
                "더 이상 이용할 수 없는 도서입니다."));
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

    @ParameterizedTest
    @ValueSource(strings = {"uk_members_nickname", "members.uk_members_nickname"})
    @DisplayName("닉네임 유일성 제약 충돌을 400과 기존 중복 메시지로 변환한다")
    void handleDuplicateNickname(String constraintName) {
        var violation = new ConstraintViolationException("duplicate", new SQLException("duplicate"), constraintName);
        var exception = new DataIntegrityViolationException("저장 실패", violation);

        var response = handler.handleDataIntegrityViolation(exception);
        var body = response.getBody();

        assertNotNull(body, "닉네임 중복 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "닉네임 중복은 400이어야 한다"),
                () -> assertEquals("INVALID_REQUEST", body.code(), "기존 잘못된 요청 코드를 반환해야 한다"),
                () -> assertEquals("이미 사용 중인 닉네임입니다.", body.message(), "기존 중복 안내를 반환해야 한다"));
    }

    @Test
    @DisplayName("다른 DB 제약 위반은 닉네임 중복으로 오인하지 않는다")
    void handleOtherConstraintViolation() {
        var violation = new ConstraintViolationException("foreign key", new SQLException("foreign key"), "fk_other");
        var exception = new DataIntegrityViolationException("다른 무결성 오류", violation);

        var response = handler.handleDataIntegrityViolation(exception);

        assertNotNull(response.getBody(), "무결성 오류 응답에는 본문이 있어야 한다");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "다른 무결성 오류는 500이어야 한다");
        assertEquals("INTERNAL_ERROR", response.getBody().code(), "다른 무결성 오류를 닉네임 중복으로 처리하면 안 된다");
    }

    @Test
    @DisplayName("처리되지 않은 검사 예외를 500과 INTERNAL_ERROR로 변환한다")
    void handleCheckedException() {
        var exception = new Exception("검사 예외 원문");

        var response = handler.handleException(exception);
        var body = response.getBody();

        assertNotNull(body, "예외 변환 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(
                        HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "처리되지 않은 예외는 500이어야 한다"),
                () -> assertEquals("INTERNAL_ERROR", body.code(), "내부 오류 코드를 반환해야 한다"),
                () -> assertEquals("검사 예외 원문", body.message(), "예외 메시지를 변경하지 않고 전달해야 한다"));
    }

    @Test
    @DisplayName("처리되지 않은 런타임 예외를 공통 예외 핸들러로 변환한다")
    void handleUnhandledRuntimeException() {
        var exception = new RuntimeException("런타임 예외 원문");

        var response = handler.handleException(exception);
        var body = response.getBody();

        assertNotNull(body, "예외 변환 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(
                        HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "처리되지 않은 예외는 500이어야 한다"),
                () -> assertEquals("INTERNAL_ERROR", body.code(), "내부 오류 코드를 반환해야 한다"),
                () -> assertEquals("런타임 예외 원문", body.message(), "예외 메시지를 변경하지 않고 전달해야 한다"));
    }

    @Test
    @DisplayName("구체적인 예외 핸들러가 공통 예외 핸들러보다 우선한다")
    void preferSpecificExceptionHandler() {
        var resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);

        var illegalArgumentHandler = resolver.resolveMethod(new IllegalArgumentException("잘못된 요청"));
        var illegalStateHandler = resolver.resolveMethod(new IllegalStateException("잘못된 상태"));
        var genericHandler = resolver.resolveMethod(new RuntimeException("서버 오류"));

        assertAll(
                () -> assertEquals(
                        "handleIllegalArgument", illegalArgumentHandler.getName(), "구체적인 400 핸들러가 우선해야 한다"),
                () -> assertEquals(
                        "handleException", illegalStateHandler.getName(), "IllegalStateException은 공통 핸들러가 처리해야 한다"),
                () -> assertEquals("handleException", genericHandler.getName(), "처리되지 않은 예외는 공통 핸들러가 처리해야 한다"));
    }

}
