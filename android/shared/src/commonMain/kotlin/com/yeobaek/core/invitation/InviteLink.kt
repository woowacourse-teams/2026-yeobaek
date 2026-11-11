package com.yeobaek.core.invitation

sealed interface InviteLinkResult {
    data class Valid(val code: String, val attribution: InviteAttribution? = null) : InviteLinkResult
    data object Invalid : InviteLinkResult
    data object Unrelated : InviteLinkResult
}

data class InviteAttribution(val shareId: String, val webVisitId: String? = null) {
    val properties: Map<String, Any> get() = buildMap {
        put("invite_share_id", shareId)
        webVisitId?.let { put("invite_web_visit_id", it) }
    }
}

object InviteLink {
    private const val PREFIX = "yeobaek://invite?code="
    private const val SHARE_PREFIX = "https://yeobaek-invite.vercel.app/?code="
    private val codePattern = Regex("[A-Z0-9]{6}")
    private val attributionIdPattern = "[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"
    private val appQueryPattern = Regex(
        "([A-Z0-9]{6})(?:&share_id=($attributionIdPattern)(?:&web_visit_id=($attributionIdPattern))?)?",
    )

    fun isValidCode(code: String): Boolean = codePattern.matches(code)

    fun create(code: String, attribution: InviteAttribution? = null): String {
        require(isValidCode(code)) { "Invalid invitation code format" }
        return buildString {
            append(PREFIX)
            append(code)
            attribution?.let {
                append("&share_id=")
                append(it.shareId)
                it.webVisitId?.let { id ->
                    append("&web_visit_id=")
                    append(id)
                }
            }
        }
    }

    fun createShareUrl(code: String, shareId: String? = null): String {
        require(isValidCode(code)) { "Invalid invitation code format" }
        return SHARE_PREFIX + code + (shareId?.let { "&share_id=$it" }.orEmpty())
    }

    fun parse(uri: String?): InviteLinkResult = when {
        uri == null || !uri.startsWith("yeobaek:") -> InviteLinkResult.Unrelated

        !uri.startsWith(PREFIX) -> InviteLinkResult.Invalid

        else -> appQueryPattern.matchEntire(uri.removePrefix(PREFIX))?.let { match ->
            val (code, shareId, webVisitId) = match.destructured
            InviteLinkResult.Valid(
                code = code,
                attribution = shareId.takeIf(String::isNotEmpty)?.let {
                    InviteAttribution(it, webVisitId.takeIf(String::isNotEmpty))
                },
            )
        } ?: InviteLinkResult.Invalid
    }

    fun shareText(groupName: String, code: String, shareId: String? = null): String =
        "${groupName.trim()} 모임에 초대해요.\n\n" +
            "초대코드: $code\n" +
            "초대 링크: ${createShareUrl(code, shareId)}\n\n" +
            "링크가 열리지 않으면 여백 앱에서 초대코드를 입력해주세요."
}
