package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.PublicRoomTabUiState
import com.yeobaek.feature.home.RecentPublicRoomListVisit
import com.yeobaek.feature.home.component.PublicGroupSection
import com.yeobaek.feature.home.model.PublicRoomBookUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecentPublicRoomAnalyticsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun horizontalSwipeRecordsOneScrollForTheVisibleVisit() {
        val events = mutableListOf<AnalyticsEvent>()
        val rooms = (1L..15L).map { id -> PublicRoomBookUiModel(id, id, "책 $id", "작가", null) }
        rule.setContent {
            val visit = remember { RecentPublicRoomListVisit { events.add(it) } }
            YeobaekTheme {
                PublicGroupSection(
                    uiState = PublicRoomTabUiState(visitedPublicRooms = rooms, screenState = ScreenState.Success),
                    updateSearchTextValue = {},
                    onPublicRoomClick = { _, _ -> },
                    onListObserved = { _, _ -> },
                    onListUserScrolled = {},
                    observationEpoch = 0,
                    recentListVisit = visit,
                )
            }
        }

        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
            .performTouchInput {
                swipeLeft()
                swipeLeft()
            }

        rule.runOnIdle {
            val viewed = events.single { it.name == "recent_public_room_list_viewed" }
            val scrolled = events.single { it.name == "recent_public_room_list_scrolled" }
            assertEquals(true, viewed.properties["has_next_card_peek"])
            assertEquals(viewed.properties["recent_list_visit_id"], scrolled.properties["recent_list_visit_id"])
        }
    }
}
