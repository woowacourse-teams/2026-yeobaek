package yeobaek.backend.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 응답 바디의 code 값. 클라이언트는 message가 아닌 이 코드로 분기한다 (API.md 0장).
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST("잘못된 요청입니다."),
    MEMBER_NOT_FOUND("존재하지 않는 회원입니다."),
    BOOK_NOT_FOUND("존재하지 않는 도서입니다."),
    BOOK_NOT_AVAILABLE("더 이상 이용할 수 없는 도서입니다."),
    CLUB_NOT_FOUND("존재하지 않는 모임입니다."),
    JOIN_CODE_NOT_FOUND("존재하지 않는 참여 코드입니다."),
    PASSAGE_NOT_FOUND("존재하지 않는 본문입니다."),
    SENTENCE_NOT_FOUND("존재하지 않는 문장입니다."),
    COMMENT_NOT_FOUND("존재하지 않는 댓글입니다."),
    CANNOT_BLOCK_SELF("자기 자신을 차단할 수 없습니다."),
    AUTHOR_NOT_FOUND("존재하지 않는 작가입니다."),
    DUPLICATE_AUTHOR("같은 작가가 중복 기재되었습니다."),
    AUTHOR_NAME_MISMATCH("ISNI로 찾은 기존 작가와 이름이 일치하지 않습니다."),
    DUPLICATE_BOOK("동일한 도서가 이미 존재합니다."),
    PRE_REGISTRATION_ALREADY_EXISTS("이미 사전신청한 이메일입니다."),
    RATE_LIMIT_EXCEEDED("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    NOT_CLUB_MEMBER("모임에 참여하지 않은 회원입니다."),
    NOT_COMMENT_OWNER("본인의 댓글이 아닙니다."),
    UNAUTHORIZED("관리자 인증에 실패했습니다."),
    INTERNAL_ERROR("서버 오류가 발생했습니다.");

    private final String defaultMessage;
}
