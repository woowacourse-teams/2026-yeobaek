package com.yeobaek.core.config

import com.yeobaek.core.update.UpdatePolicyPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PatchNoteTest {
    @Test
    fun usesExactVersionForInstalledPlatform() {
        assertNotNull(decodePatchNote(config(), UpdatePolicyPlatform.ANDROID, "1.6.0"))
        assertNotNull(decodePatchNote(config(), UpdatePolicyPlatform.IOS, "1.6.1"))
        assertNull(decodePatchNote(config(), UpdatePolicyPlatform.IOS, "1.6.0"))
        assertNull(decodePatchNote(config(), UpdatePolicyPlatform.ANDROID, "1.6"))
    }

    @Test
    fun skipsDisabledMalformedAndEmptyNotes() {
        assertNull(decodeForAndroid("""{"enabled":false}"""))
        assertNull(decodeForAndroid("""{"enabled":"""))
        assertNull(decodeForAndroid(config(changes = "[]")))
        assertNull(decodeForAndroid(config(changes = """[{"title":"새 기능","body":" "}]""")))
    }

    @Test
    fun keepsItemOrderAndIgnoresPreviouslyPublishedImageFields() {
        val note = assertNotNull(
            decodeForAndroid(
                config(
                    changes = """
                        [
                          {"title":"첫 번째","body":"본문","imageUrl":"https://example.com/reader.jpg","imageAlt":"독서 화면"},
                          {"title":"두 번째","body":"설명"}
                        ]
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(
            listOf(PatchNoteChange("첫 번째", "본문"), PatchNoteChange("두 번째", "설명")),
            note.changes,
        )
    }

    private fun decodeForAndroid(raw: String): PatchNote? =
        decodePatchNote(raw, UpdatePolicyPlatform.ANDROID, "1.6.0")

    private fun config(changes: String = """[{"title":"새 기능","body":"기능 설명"}]"""): String =
        """
        {
          "enabled": true,
          "id": "release-1.6",
          "targets": {"android": "1.6.0", "ios": "1.6.1"},
          "title": "여백 업데이트 소식",
          "changes": $changes
        }
        """.trimIndent()
}
