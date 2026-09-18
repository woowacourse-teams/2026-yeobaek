package yeobaek.backend.support;

/**
 * 에러 응답 바디의 code 값. 코드 의미는 API.md 0장에 기록하고,
 * message는 예외 발생 지점에서 요청 문맥에 맞게 작성한다.
 */
public enum ErrorCode {

    INVALID_REQUEST,
    MEMBER_NOT_FOUND,
    BOOK_NOT_FOUND,
    BOOK_NOT_AVAILABLE,
    CLUB_NOT_FOUND,
    JOIN_CODE_NOT_FOUND,
    PASSAGE_NOT_FOUND,
    SENTENCE_NOT_FOUND,
    COMMENT_NOT_FOUND,
    CANNOT_REPORT_OWN_COMMENT,
    CANNOT_BLOCK_SELF,
    AUTHOR_NOT_FOUND,
    DUPLICATE_AUTHOR,
    AUTHOR_NAME_MISMATCH,
    DUPLICATE_BOOK,
    NOT_CLUB_MEMBER,
    NOT_COMMENT_OWNER,
    UNAUTHORIZED,
    INTERNAL_ERROR
}
