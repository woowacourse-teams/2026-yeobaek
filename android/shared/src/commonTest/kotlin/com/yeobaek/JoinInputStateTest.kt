package com.yeobaek

import androidx.lifecycle.SavedStateHandle
import com.yeobaek.feature.group.join.JoinInputState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoinInputStateTest {
    @Test
    fun prefillDoesNotSubmitAndOnlyANewRequestReplacesUserEdits() {
        val state = JoinInputState(SavedStateHandle())
        state.initialize("ABC123", "first")
        assertEquals("ABC123", state.value.codeValue)
        assertFalse(state.value.isJoining)
        state.edit("EDIT12")
        state.initialize("ABC123", "first")
        assertEquals("EDIT12", state.value.codeValue)
        state.initialize("DEF456", "second")
        assertEquals("DEF456", state.value.codeValue)
    }

    @Test
    fun restoredInputWinsOverOriginalLinkAndInFlightWorkIsNotRestored() {
        val saved = SavedStateHandle()
        val first = JoinInputState(saved)
        first.initialize("ABC123", "request")
        first.edit("EDIT12")
        first.beginJoin()
        val restored = JoinInputState(saved)
        restored.initialize("ABC123", "request")
        assertEquals("EDIT12", restored.value.codeValue)
        assertFalse(restored.value.isJoining)
    }

    @Test
    fun normalEntryStaysEmptyAndDoesNotEraseEditsOnRecomposition() {
        val state = JoinInputState(SavedStateHandle())
        state.initialize(null, null)
        assertEquals("", state.value.codeValue)
        state.edit("MANUAL")
        state.initialize(null, null)
        assertEquals("MANUAL", state.value.codeValue)
    }

    @Test
    fun submissionCapturesCodeAndBlocksDuplicateTapsAndInputChanges() {
        val state = JoinInputState(SavedStateHandle())
        state.initialize("ABC123", "one")
        assertEquals("ABC123", state.beginJoin())
        assertNull(state.beginJoin())
        state.edit("DEF456")
        state.initialize("DEF456", "two")
        assertEquals("ABC123", state.value.codeValue)
        state.finishJoin(success = false)
        assertTrue(state.value.codeState)
        assertEquals("ABC123", state.value.codeValue)
        assertEquals("ABC123", state.beginJoin())
        state.finishJoin(success = true)
        assertNull(state.beginJoin())
    }

    @Test
    fun malformedCodeNeverStartsSubmission() {
        val state = JoinInputState(SavedStateHandle())
        state.initialize("invalid", "request")
        assertEquals("", state.value.codeValue)
        state.edit("ABC")
        assertNull(state.beginJoin())
        assertTrue(state.value.codeState)
        assertFalse(state.value.isJoining)
    }
}
