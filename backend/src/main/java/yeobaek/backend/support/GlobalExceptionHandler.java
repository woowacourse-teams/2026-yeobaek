package yeobaek.backend.support;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import yeobaek.backend.appreciation.api.CommentFailure;
import yeobaek.backend.application.appreciation.CommentPolicyFailure;
import yeobaek.backend.content.api.ContentUnavailableFailure;
import yeobaek.backend.content.api.ContentNotFoundFailure;
import yeobaek.backend.content.api.ContentBodyUnsupportedFailure;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation() {
        logExpected("exception.handleValidation", ErrorCode.INVALID_REQUEST, Map.of("validation", "bean"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "필수 요청 값이 누락되었습니다."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        logExpected("exception.handleIllegalArgument", ErrorCode.INVALID_REQUEST,
                Map.of("exceptionType", e.getClass().getSimpleName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        logExpected("exception.handleInvalidRequest", ErrorCode.INVALID_REQUEST, e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        logExpected("exception.handleUnreadableBody", ErrorCode.INVALID_REQUEST,
                Map.of("exceptionType", e.getClass().getSimpleName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        logExpected("exception.handleMissingParameter", ErrorCode.INVALID_REQUEST,
                Map.of("parameterName", e.getParameterName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "필수 파라미터가 없습니다: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        logExpected("exception.handleTypeMismatch", ErrorCode.INVALID_REQUEST, Map.of("parameterName", e.getName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "파라미터 형식이 올바르지 않습니다: " + e.getName()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        logExpected("exception.handleBadRequest", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        logExpected("exception.handleNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        logExpected("exception.handleForbidden", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(CommentFailure.class)
    public ResponseEntity<ErrorResponse> handleCommentFailure(CommentFailure e) {
        ErrorCode code = switch (e.reason()) {
            case NOT_FOUND, NOT_VISIBLE -> ErrorCode.COMMENT_NOT_FOUND;
            case NOT_OWNER -> ErrorCode.NOT_COMMENT_OWNER;
            case CANNOT_REPORT_OWN_COMMENT -> ErrorCode.CANNOT_REPORT_OWN_COMMENT;
        };
        HttpStatus status = e.reason() == CommentFailure.Reason.NOT_OWNER
                ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        Map<String, String> context = Map.of("commentId", Long.toString(e.commentId()));
        logExpected("exception.handleCommentFailure", code, context);
        return ResponseEntity.status(status).body(ErrorResponse.of(code, e.getMessage()));
    }

    @ExceptionHandler(CommentPolicyFailure.class)
    public ResponseEntity<ErrorResponse> handleCommentPolicyFailure(CommentPolicyFailure e) {
        if (e.reason() == CommentPolicyFailure.Reason.SPACE_ACCESS_DENIED) {
            logExpected("exception.handleCommentPolicyFailure", ErrorCode.NOT_CLUB_MEMBER,
                    Map.of("reason", e.reason().name()));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of(ErrorCode.NOT_CLUB_MEMBER, e.getMessage()));
        }
        logExpected("exception.handleCommentPolicyFailure", ErrorCode.INVALID_REQUEST,
                Map.of("reason", e.reason().name()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(ContentUnavailableFailure.class)
    public ResponseEntity<ErrorResponse> handleContentUnavailable(ContentUnavailableFailure e) {
        logExpected("exception.handleContentUnavailable", ErrorCode.BOOK_NOT_AVAILABLE,
                Map.of("contentId", Long.toString(e.failedContentId())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.BOOK_NOT_AVAILABLE, e.getMessage()));
    }

    @ExceptionHandler(ContentNotFoundFailure.class)
    public ResponseEntity<ErrorResponse> handleContentNotFound(ContentNotFoundFailure e) {
        logExpected("exception.handleContentNotFound", ErrorCode.BOOK_NOT_FOUND,
                Map.of("contentId", Long.toString(e.failedContentId())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.BOOK_NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(ContentBodyUnsupportedFailure.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedContentBody(ContentBodyUnsupportedFailure e) {
        logExpected("exception.handleUnsupportedContentBody", ErrorCode.INVALID_REQUEST,
                Map.of("contentId", Long.toString(e.failedContentId())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        logExpected("exception.handleUnauthorized", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        logUnexpected("exception.handleIllegalState", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, e.getMessage()));
    }

    private void logExpected(String operation, ErrorCode errorCode, Map<String, String> context) {
        var event = LOGGER.atInfo()
                .addKeyValue(OPERATION, operation)
                .addKeyValue(RESULT, "rejected")
                .addKeyValue("errorCode", errorCode.name());
        context.forEach(event::addKeyValue);
        event.log("의도한 요청 거절을 처리했습니다.");
    }

    private void logUnexpected(String operation, Exception exception) {
        LOGGER.atError()
                .addKeyValue(OPERATION, operation)
                .addKeyValue(RESULT, "failure")
                .addKeyValue("errorCode", ErrorCode.INTERNAL_ERROR.name())
                .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                .setCause(exception)
                .log("예상하지 못한 서버 오류를 처리했습니다.");
    }
}
