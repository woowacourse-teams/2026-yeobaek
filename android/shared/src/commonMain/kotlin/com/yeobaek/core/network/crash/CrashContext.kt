package com.yeobaek.core.network.crash

/**
 * Crashlytics에 전송해도 되는 재현용 상태만 표현한다.
 *
 * 사용자 입력 문자열, 닉네임, 댓글, 초대 코드, 회원 ID, 인증 정보는 이 타입에 추가하지 않는다.
 */
data class CrashContext(
    val screen: CrashScreen,
    val operation: CrashOperation,
    val bookId: Long? = null,
    val chapterSequence: Int? = null,
    val passageSequence: Int? = null,
    val itemCount: Int? = null,
)
