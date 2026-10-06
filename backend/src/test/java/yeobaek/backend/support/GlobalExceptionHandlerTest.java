package yeobaek.backend.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.application.appreciation.CommentPolicyException;
import yeobaek.backend.application.appreciation.CommentQueryException;
import yeobaek.backend.application.content.ContentReadingPolicyException;
import yeobaek.backend.application.reading.ReadingProgressPolicyException;
import yeobaek.backend.application.space.query.SpaceQueryPolicyException;
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.collaboration.api.binding.ContentBindingNotFoundException;
import yeobaek.backend.shared.exception.BadRequestException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.ForbiddenException;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.shared.exception.UnauthorizedException;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberNotFoundException;
import yeobaek.backend.space.api.club.ClubMembershipNotFoundException;
import yeobaek.backend.space.api.club.JoinCodeAllocationException;

import java.util.Map;
import java.util.stream.Stream;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private static final String BUSINESS_MESSAGE = "업무 요청을 처리할 수 없습니다.";

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

    @Test
    void mapsCanonicalMemberNotFoundException() {
        var response = handler.handleMemberNotFound(new MemberNotFoundException(new MemberId(7L),
                "회원이 존재하지 않습니다: memberId=7"));

        assertError(response, HttpStatus.BAD_REQUEST, "MEMBER_NOT_FOUND");
    }

    @Test
    void mapsCanonicalContentBindingNotFoundException() {
        var response = handler.handleContentBindingNotFound(new ContentBindingNotFoundException(
                "공간에 연결된 콘텐츠가 없습니다: spaceId=8", Map.of("spaceId", "8")));

        assertError(response, HttpStatus.BAD_REQUEST, "CONTENT_NOT_BOUND");
    }

    @Test
    void mapsCanonicalClubMembershipNotFoundException() {
        var response = handler.handleClubMembershipNotFound(
                new ClubMembershipNotFoundException(new MemberId(7L), new SpaceId(8L),
                        "가입 이력이 없습니다: actorId=7, spaceId=8"));

        assertError(response, HttpStatus.BAD_REQUEST, "NOT_CLUB_MEMBER");
    }

    @Test
    @DisplayName("공간 접근 거절은 예외의 코드와 메시지를 유지하며 400으로 변환한다")
    void preserveBusinessCustomExceptionContract() {
        var exception = new CommentPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                "댓글을 작성할 공간에 접근할 수 없습니다: spaceId=8",
                Map.of("actorId", "7", "spaceId", "8", "reason", "SPACE_ACCESS_DENIED"));

        var response = handler.handleCommentPolicyException(exception);

        assertNotNull(response.getBody(), "인가 실패 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "업무상 접근 거절은 400이어야 한다"),
                () -> assertEquals("SPACE_ACCESS_DENIED", response.getBody().code(), "예외 코드를 유지해야 한다"),
                () -> assertEquals(exception.getMessage(), response.getBody().message(), "예외 메시지를 유지해야 한다"));
    }

    @Test
    @DisplayName("서버 오류도 예외의 커스텀 코드와 메시지를 유지하며 500으로 변환한다")
    void preserveServerCustomExceptionContract() {
        var exception = new JoinCodeAllocationException(
                "참여 코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.", Map.of("attemptCount", "5"));

        var response = handler.handleLogContextException(exception);

        assertNotNull(response.getBody(), "서버 오류 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "서버 오류는 500이어야 한다"),
                () -> assertEquals("INTERNAL_ERROR", response.getBody().code(), "예외 코드를 유지해야 한다"),
                () -> assertEquals(exception.getMessage(), response.getBody().message(), "예외 메시지를 유지해야 한다"));
    }

    @Test
    @DisplayName("별도 핸들러가 없는 Exception도 공통 오류 본문과 500으로 응답한다")
    void handleUnhandledExceptionThroughMvc() throws Exception {
        var exception = new Exception("처리되지 않은 서버 오류 원문");
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController(exception))
                .setControllerAdvice(handler).build();

        var result = mvc.perform(get("/test/unhandled-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"code":"INTERNAL_ERROR","message":"처리되지 않은 서버 오류 원문"}
                        """, org.springframework.test.json.JsonCompareMode.STRICT))
                .andReturn();

        assertSame(exception, result.getResolvedException(), "원본 예외가 공통 핸들러에서 처리되어야 한다");
    }

    @ParameterizedTest
    @MethodSource("businessExceptions")
    @DisplayName("업무상 접근과 소유권 거절은 모두 400이며 커스텀 코드와 메시지를 유지한다")
    void businessExceptionsReturnBadRequest(Exception exception, ErrorCode code) throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController(exception))
                .setControllerAdvice(handler).build();

        mvc.perform(get("/test/unhandled-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(BUSINESS_MESSAGE));
    }

    @Test
    @DisplayName("회원 인증 실패는 401과 인증 오류 코드를 유지한다")
    void authenticationFailureRemainsUnauthorized() {
        var response = handler.handleUnauthorized(new UnauthorizedException(ErrorCode.UNAUTHORIZED,
                "회원 헤더를 식별할 수 없습니다."));

        assertError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    @Test
    @DisplayName("참여 코드 미존재는 400과 대상별 오류 코드 및 메시지를 반환한다")
    void missingJoinCodeReturnsMeaningfulError() {
        var response = handler.handleNotFound(new NotFoundException(ErrorCode.JOIN_CODE_NOT_FOUND,
                "참여 코드에 해당하는 모임을 찾을 수 없습니다.", Map.of("actorId", "7", "lookup", "joinCode")));

        assertError(response, HttpStatus.BAD_REQUEST, "JOIN_CODE_NOT_FOUND");
        assertEquals("참여 코드에 해당하는 모임을 찾을 수 없습니다.", response.getBody().message(),
                "조회 실패 지점의 메시지를 그대로 반환해야 한다");
    }

    private static Stream<Arguments> businessExceptions() {
        var denied = ErrorCode.SPACE_ACCESS_DENIED;
        var context = Map.of("actorId", "7", "spaceId", "8");
        return Stream.of(
                Arguments.of(new ForbiddenException(denied, BUSINESS_MESSAGE, context), denied),
                Arguments.of(new CommentPolicyException(denied, BUSINESS_MESSAGE, context), denied),
                Arguments.of(new CommentQueryException(denied, BUSINESS_MESSAGE, context), denied),
                Arguments.of(new ContentReadingPolicyException(denied, BUSINESS_MESSAGE, context), denied),
                Arguments.of(new ReadingProgressPolicyException(denied, BUSINESS_MESSAGE, context), denied),
                Arguments.of(new SpaceQueryPolicyException(denied, BUSINESS_MESSAGE, context), denied),
                Arguments.of(new CommentException(ErrorCode.NOT_COMMENT_OWNER, new AppreciationId(9L),
                        BUSINESS_MESSAGE, context), ErrorCode.NOT_COMMENT_OWNER),
                Arguments.of(new ClubMembershipNotFoundException(new MemberId(7L), new SpaceId(8L),
                        BUSINESS_MESSAGE), ErrorCode.NOT_CLUB_MEMBER));
    }

    @RestController
    static class FailingController {

        private final Exception exception;

        FailingController(Exception exception) {
            this.exception = exception;
        }

        @GetMapping("/test/unhandled-exception")
        public void fail() throws Exception {
            throw exception;
        }
    }

    private void assertError(org.springframework.http.ResponseEntity<ErrorResponse> response,
                             HttpStatus status, String code) {
        assertNotNull(response.getBody(), "예외 변환 응답에는 본문이 있어야 한다");
        assertAll(
                () -> assertEquals(status, response.getStatusCode(), "기대한 HTTP 상태를 반환해야 한다"),
                () -> assertEquals(code, response.getBody().code(), "기대한 오류 코드를 반환해야 한다"),
                () -> assertNotNull(response.getBody().message(), "오류 메시지가 있어야 한다"));
    }

}
