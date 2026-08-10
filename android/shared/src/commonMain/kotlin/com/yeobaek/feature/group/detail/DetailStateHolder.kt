package com.yeobaek.feature.group.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.mock.group.MockGroupData
import com.yeobaek.data.mock.group.MockUserData
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel

class DetailStateHolder(
    private val groupId: String,
    private val mockGroupData: List<MockGroupData>
) {
    var uiState: DetailUiState by mutableStateOf(DetailUiState())
        private set

    init {
        initGroupData()
    }

    private fun initGroupData() {
        val groupData = mockGroupData.find { it.groupCode == groupId }
        uiState = uiState.copy(
            bookUiModel = DetailBookUiModel(
                uri = groupData?.uri ?: "",
                title = groupData?.title ?: "",
                author = groupData?.author ?: "",
                currentProgress = groupData?.progressRate ?: 0f,
            ),
            groupUiModel = GroupUiModel(
                name = groupData?.groupName ?: "",
                groupCode = groupData?.groupCode ?: "",
                users = groupData?.users?.map {
                    UserUiModel(
                        name = it.name,
                    )
                } ?: emptyList()
            )
        )
    }
}
