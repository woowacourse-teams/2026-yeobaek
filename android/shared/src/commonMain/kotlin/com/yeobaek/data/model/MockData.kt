package com.yeobaek.data.model

object MockData {
    val theLittlePrince = BookModel(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2977195",
        title = "어린 왕자",
        author = "앙투안 드 생텍쥐페리",
        progressRate = 0.3f,
    )

    val demian = BookModel(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
        title = "데미안",
        author = "헤르만 헤세",
        progressRate = 0.12f,
    )

    val group1 = GroupModel(
        groupCode = "BOOK42",
        groupName = "어른이들을 위한 동화 읽기",
        book = theLittlePrince,
        users = (1..8).map {
            UserModel(
                name = "하로${it}"
            )
        }
    )

    val group2 = GroupModel(
        groupCode = "YEOBAEK",
        groupName = "고전 읽는 오후 모임",
        book = demian,
        users = (1..4).map {
            UserModel(
                name = "엘리${it}"
            )
        }
    )

    val mockGroups = listOf(group1, group2)
}
