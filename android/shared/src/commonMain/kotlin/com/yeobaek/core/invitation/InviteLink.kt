package com.yeobaek.core.invitation

sealed interface InviteLinkResult {
    data class Valid(val code: String) : InviteLinkResult
    data object Invalid : InviteLinkResult
    data object Unrelated : InviteLinkResult
}

object InviteLink {
    private const val PREFIX = "yeobaek://invite?code="
    private const val SHARE_PREFIX = "https://yeobaek-invite.vercel.app/?code="
    private val codePattern = Regex("[A-Z0-9]{6}")

    fun isValidCode(code: String): Boolean = codePattern.matches(code)

    fun create(code: String): String {
        require(isValidCode(code)) { "Invalid invitation code format" }
        return PREFIX + code
    }

    fun createShareUrl(code: String): String {
        require(isValidCode(code)) { "Invalid invitation code format" }
        return SHARE_PREFIX + code
    }

    fun parse(uri: String?): InviteLinkResult = when {
        uri == null || !uri.startsWith("yeobaek:") -> InviteLinkResult.Unrelated

        !uri.startsWith(PREFIX) -> InviteLinkResult.Invalid

        else -> uri.removePrefix(PREFIX).let { code ->
            if (isValidCode(code)) InviteLinkResult.Valid(code) else InviteLinkResult.Invalid
        }
    }

    fun shareText(groupName: String, code: String): String =
        "${groupName.trim()} 모임에 초대해요.\n\n" +
            "초대코드: $code\n" +
            "초대 링크: ${createShareUrl(code)}\n\n" +
            "링크가 열리지 않으면 여백 앱에서 초대코드를 입력해주세요."
}
