package com.yeobaek.feature.group.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.model.GroupModel
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel

class DetailStateHolder(
    private val groupId: String,
    private val groupModelList: List<GroupModel>,
) {
    var uiState: DetailUiState by mutableStateOf(DetailUiState())
        private set

    init {
        initGroupData()
    }

    private fun initGroupData() {
        val groupData = groupModelList.find { it.groupCode == groupId } ?: return
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
