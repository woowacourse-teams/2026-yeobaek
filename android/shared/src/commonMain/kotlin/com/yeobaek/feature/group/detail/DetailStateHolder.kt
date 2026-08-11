package com.yeobaek.feature.group.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel

class DetailStateHolder(
    groupRepository: GroupRepository,
) {
    var uiState: DetailUiState by mutableStateOf(DetailUiState())
        private set

    val groups: List<GroupModel> = groupRepository.getGroups()

    fun initGroupData(groupCode: String) {
        val groupData = groups.find { it.groupCode == groupCode } ?: throw IllegalArgumentException("모임이 없음요")

        uiState = uiState.copy(
            bookUiModel = DetailBookUiModel(
                uri = groupData.book.uri,
                title = groupData.book.title,
                author = groupData.book.author,
                currentProgress = groupData.book.progressRate,
            ),
            groupUiModel = GroupUiModel(
                name = groupData.groupName,
                groupCode = groupData.groupCode,
                users = groupData.users.map {
                    UserUiModel(
                        name = it.name,
                    )
                },
            ),
        )
    }
}
