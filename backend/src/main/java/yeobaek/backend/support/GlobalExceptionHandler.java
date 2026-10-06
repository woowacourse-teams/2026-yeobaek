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
import yeobaek.backend.appreciation.api.comment.CommentException;
import yeobaek.backend.application.appreciation.CommentPolicyException;
import yeobaek.backend.content.api.ContentUnavailableException;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.content.api.body.ContentBodyUnsupportedException;
import yeobaek.backend.content.api.location.ContentLocationNotFoundException;
import yeobaek.backend.collaboration.api.binding.ContentBindingNotFoundException;
import yeobaek.backend.member.api.MemberNotFoundException;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.club.ClubMembershipNotFoundException;
import yeobaek.backend.application.reading.ReadingProgressPolicyException;
import yeobaek.backend.application.appreciation.CommentQueryException;
import yeobaek.backend.application.content.ContentReadingPolicyException;
import yeobaek.backend.application.space.query.SpaceQueryPolicyException;
import yeobaek.backend.application.space.query.UnsupportedSpaceQueryException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.BadRequestException;
import yeobaek.backend.shared.exception.ForbiddenException;
import yeobaek.backend.shared.exception.InvalidRequestException;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.shared.exception.UnauthorizedException;

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
        logExpected("exception.handleInvalidRequest", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(CommentException.class)
    public ResponseEntity<ErrorResponse> handleCommentException(CommentException e) {
        logExpected("exception.handleCommentFailure", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(CommentPolicyException.class)
    public ResponseEntity<ErrorResponse> handleCommentPolicyException(CommentPolicyException e) {
        logExpected("exception.handleCommentPolicyFailure", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ContentUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleContentUnavailable(ContentUnavailableException e) {
        logExpected("exception.handleContentUnavailable", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentNotFound(ContentNotFoundException e) {
        logExpected("exception.handleContentNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ContentLocationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentLocationNotFound(ContentLocationNotFoundException e) {
        logExpected("exception.handleContentLocationNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(SpaceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSpaceNotFound(SpaceNotFoundException e) {
        logExpected("exception.handleSpaceNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException e) {
        logExpected("exception.handleMemberNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ContentBindingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentBindingNotFound(ContentBindingNotFoundException e) {
        logExpected("exception.handleContentBindingNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ClubMembershipNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClubMembershipNotFound(ClubMembershipNotFoundException e) {
        logExpected("exception.handleClubMembershipNotFound", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ReadingProgressPolicyException.class)
    public ResponseEntity<ErrorResponse> handleReadingProgressPolicy(ReadingProgressPolicyException e) {
        logExpected("exception.handleReadingProgressPolicy", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(CommentQueryException.class)
    public ResponseEntity<ErrorResponse> handleCommentQueryException(CommentQueryException e) {
        logExpected("exception.handleCommentQueryFailure", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ContentReadingPolicyException.class)
    public ResponseEntity<ErrorResponse> handleContentReadingPolicy(ContentReadingPolicyException e) {
        logExpected("exception.handleContentReadingPolicy", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(SpaceQueryPolicyException.class)
    public ResponseEntity<ErrorResponse> handleSpaceQueryPolicy(SpaceQueryPolicyException e) {
        logExpected("exception.handleSpaceQueryPolicy", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(UnsupportedSpaceQueryException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedSpaceQuery(UnsupportedSpaceQueryException e) {
        logExpected("exception.handleUnsupportedSpaceQuery", e.getCode(), e.getLogContext());
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ContentBodyUnsupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedContentBody(ContentBodyUnsupportedException e) {
        logExpected("exception.handleUnsupportedContentBody", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        logExpected("exception.handleUnauthorized", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(LogContextException.class)
    public ResponseEntity<ErrorResponse> handleLogContextException(LogContextException e) {
        if (e.getCode() == ErrorCode.INTERNAL_ERROR) {
            logUnexpected("exception.handleLogContextException", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(e.getCode(), e.getMessage()));
        }
        logExpected("exception.handleLogContextException", e.getCode(), e.getLogContext());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        logUnexpected("exception.handleIllegalState", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        logUnexpected("exception.handleException", e);
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
        var event = LOGGER.atError()
                .addKeyValue(OPERATION, operation)
                .addKeyValue(RESULT, "failure")
                .addKeyValue("errorCode", exception instanceof LogContextException custom
                        ? custom.getCode().name() : ErrorCode.INTERNAL_ERROR.name())
                .addKeyValue("exceptionType", exception.getClass().getSimpleName());
        if (exception instanceof LogContextException custom) {
            custom.getLogContext().forEach(event::addKeyValue);
        }
        event.setCause(exception).log("예상하지 못한 서버 오류를 처리했습니다.");
    }
}
