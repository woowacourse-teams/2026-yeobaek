package com.yeobaek.data.mock.group

data class MockGroupData(
    val groupCode: String = "",
    val uri: String = "",
    val title: String = "",
    val author: String = "",
    val progressRate: Float = 0f,
    val groupName: String = "",
    val users: List<MockUserData> = emptyList()
) {
    companion object {
        val mockGroupData = listOf(
            MockGroupData(
                groupCode = "BOOK42",
                uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2977195",
                title = "어린 왕자",
                author = "앙투안 드 생텍쥐페리",
                progressRate = 0.3f,
                groupName = "어른이들을 위한 동화 읽기",
                users = (1..8).map {
                    MockUserData(
                        name = "하로${it}"
                    )
                }
            ),
            MockGroupData(
                groupCode = "YEOBAEK",
                uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                title = "데미안",
                author = "헤르만 헤세",
                progressRate = 0.12f,
                groupName = "고전 읽는 오후 모임",
                users = (1..4).map {
                    MockUserData(
                        name = "엘리${it}"
                    )
                }
            )
        )
    }
}
