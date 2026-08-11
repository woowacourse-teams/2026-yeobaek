package com.yeobaek.data

import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel

object MockData {
    val theLittlePrince = BookModel(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2977195",
        title = "어린 왕자",
        author = "앙투안 드 생텍쥐페리",
        progressRate = 0.3f,
        description = "어린 왕자가 성장하면 어른 왕자",
    )

    val demian = BookModel(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
        title = "데미안",
        author = "헤르만 헤세",
        progressRate = 0.12f,
        description = "깨달음을 주는 소설이라고 한다"
    )

    val theMidnightLibrary = BookModel(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791191056556.jpg?t=2977270",
        title = "미드나잇 라이브러리",
        author = "매트 헤이그",
        progressRate = 0.0f,
        description = "삶의 가능성을 다시 바라보는 이야기",
    )

    val almond = BookModel(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9788936434267.jpg?t=2977220",
        title = "아몬드",
        author = "손원평",
        progressRate = 0.0f,
        description = "마음을 이해하고 성장하는 소설",
    )

    val theSecondChanceConvenienceStore = BookModel(
        uri = "https://cdn.kids.donga.com/news/photo/202304/159384_246405_3011.jpg",
        title = "불편한 편의점",
        author = "김호연",
        progressRate = 0.0f,
        description = "낯선 이들이 건네는 다정한 위로",
    )

    val group1 = GroupModel(
        groupCode = "BOOK42",
        groupName = "어른이들을 위한 동화 읽기",
        book = theLittlePrince,
        users = (1..8).map {
            UserModel(
                name = "하로${it}",
            )
        },
    )

    val group2 = GroupModel(
        groupCode = "YEOBAEK",
        groupName = "고전 읽는 오후 모임",
        book = demian,
        users = (1..4).map {
            UserModel(
                name = "엘리${it}",
            )
        },
    )

    val mockGroups = listOf(group1, group2)

    val mockBooks = listOf(
        theLittlePrince,
        demian,
        theMidnightLibrary,
        almond,
        theSecondChanceConvenienceStore,
    )
}
