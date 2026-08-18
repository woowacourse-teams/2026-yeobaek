package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.PassageCommentUiModel
import com.yeobaek.feature.reader.model.PassageUiModel

object ReaderPreviewData {
    val commentsByPassageId: Map<Int, List<PassageCommentUiModel>> = mapOf(
        2 to listOf(
            PassageCommentUiModel(
                commentId = 7,
                memberId = 2,
                nickname = "지수",
                content = "이 문장에서 멈칫했어요.",
                createdAt = "2026-08-05T14:30:00",
                updatedAt = null,
                mine = false,
            ),
            PassageCommentUiModel(
                commentId = 8,
                memberId = 3,
                nickname = "민서",
                content = "세계를 깨뜨린다는 표현이 오래 남네요.",
                createdAt = "2026-08-06T09:12:00",
                updatedAt = "2026-08-06T10:05:00",
                mine = false,
            ),
        ),
        3 to listOf(
            PassageCommentUiModel(
                commentId = 9,
                memberId = 4,
                nickname = "서준",
                content = "답장을 알아보는 순간의 긴장감이 인상 깊어요.",
                createdAt = "2026-08-07T21:03:00",
                updatedAt = null,
                mine = false,
            ),
        ),
        5 to listOf(
            PassageCommentUiModel(
                commentId = 10,
                memberId = 5,
                nickname = "하윤",
                content = "젊은 선생님을 바라보는 시선이 재미있어요.",
                createdAt = "2026-08-08T08:45:00",
                updatedAt = null,
                mine = false,
            ),
            PassageCommentUiModel(
                commentId = 11,
                memberId = 6,
                nickname = "도윤",
                content = "거짓 품위를 보이지 않았다는 말에 공감했어요.",
                createdAt = "2026-08-08T18:20:00",
                updatedAt = null,
                mine = false,
            ),
            PassageCommentUiModel(
                commentId = 12,
                memberId = 1,
                nickname = "나",
                content = "호감의 이유가 아주 선명하게 드러나는 문단 같아요.",
                createdAt = "2026-08-09T13:10:00",
                updatedAt = null,
                mine = true,
            ),
        ),
    )

    val passages: List<PassageUiModel> = listOf(
        PassageUiModel(
            passageId = 1,
            sequence = 1,
            chapterId = 1,
            content =
                "종이를 만지작거리다 아무 생각 없이 폈는데 그 안에 몇 마디 말이 적혀 " +
                    "있는 것을 보았다. 그 위로 시선을 한 번 던지고는 말 한마디에 " +
                    "사로잡혀 버렸다. 나는 놀라 읽었다. 그사이 나의 가슴은 운명 앞에서 " +
                    "큰 추위가 닥친 듯 오그라들었다.",
            commentCount = 0,
        ),
        PassageUiModel(
            passageId = 2,
            sequence = 2,
            chapterId = 1,
            content =
                "\"새는 알에서 나오려고 투쟁한다. 알은 세계이다. 태어나려는 자는 하나의 " +
                    "세계를 깨뜨려야 한다. 새는 신에게로 날아간다. 신의 이름은 아브락사스.\"",
            commentCount = 2,
        ),
        PassageUiModel(
            passageId = 3,
            sequence = 3,
            chapterId = 1,
            content =
                "이 글줄을 몇 차례 읽은 뒤 나는 깊은 생각에 빠졌다. 어떤 의심도 불가능했다. " +
                    "이것은 데미안이 보낸 답장이었다. 나와 그 말고 그 새에 대해 아는 " +
                    "사람은 있을 수 없었다.",
            commentCount = 1,
        ),
        PassageUiModel(
            passageId = 4,
            sequence = 4,
            chapterId = 1,
            content =
                "내 그림을 그가 받은 것이다. 그는 이해하였고 내가 해석하는 것을 도운 것이다. " +
                    "하지만 이 모든 것이 서로 무슨 관련이 있단 말인가? 그리고 무엇보다 " +
                    "나를 괴롭힌 것은 아브락사스란 무엇인가 하는 의문이었다. 들어 본 적도 " +
                    "읽어 본 적도 없는 말이었다. \"신의 이름은 아브락사스!\"",
            commentCount = 0,
        ),
        PassageUiModel(
            passageId = 5,
            sequence = 5,
            chapterId = 1,
            content =
                "수업을 조금도 듣지 못한 채 그 시간이 갔다. 다음 시간이 시작되었다. " +
                    "오전의 마지막 수업이었다. 그 시간은 젊은 보조 선생님 담당이었다. " +
                    "대학을 갓 졸업했는데, 그렇게 젊다는 것 그리고 우리에게 거짓 품위를 " +
                    "보이려 들지 않았다는 것만으로도 벌써 우리의 호감을 산 분이었다.",
            commentCount = 3,
        ),
    )
}
