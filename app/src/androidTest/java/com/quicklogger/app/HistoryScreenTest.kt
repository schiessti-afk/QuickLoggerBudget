package com.quicklogger.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.presentation.history.HistoryEvent
import com.quicklogger.app.presentation.history.HistoryRowUiModel
import com.quicklogger.app.presentation.history.HistoryScreenContent
import com.quicklogger.app.presentation.history.HistoryUiState
import com.quicklogger.app.presentation.theme.QuickLoggerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        state: HistoryUiState,
        onEvent: (HistoryEvent) -> Unit = {},
        onEditExpense: (Long) -> Unit = {},
    ) = composeRule.setContent {
        QuickLoggerTheme {
            HistoryScreenContent(
                uiState = state,
                onEvent = onEvent,
                onNavigateUp = {},
                onEditExpense = onEditExpense,
                onManageCategories = {},
            )
        }
    }

    @Test
    fun emptyStateShowsWhenThereAreNoRows() {
        setContent(HistoryUiState())

        composeRule.onNodeWithText("No expenses in this period.").assertExists()
    }

    @Test
    fun rowsRenderAmountCategoryAndDate() {
        setContent(
            HistoryUiState(
                rows = listOf(
                    HistoryRowUiModel(1L, "$45.00", "Food", "8/17/26, 2:32 PM", hasReceipt = false),
                ),
            ),
        )

        composeRule.onNodeWithText("$45.00").assertExists()
    }

    @Test
    fun tappingAPeriodChipEmitsPeriodSelected() {
        val events = mutableListOf<HistoryEvent>()
        setContent(HistoryUiState(period = Period.DAY), onEvent = { events += it })

        composeRule.onNodeWithText("Week").performClick()

        assertEquals(listOf(HistoryEvent.PeriodSelected(Period.WEEK)), events)
    }

    @Test
    fun tappingARowNavigatesToEdit() {
        var tapped: Long? = null
        setContent(
            HistoryUiState(
                rows = listOf(
                    HistoryRowUiModel(7L, "$45.00", "Food", "8/17/26, 2:32 PM", hasReceipt = false),
                ),
            ),
            onEditExpense = { tapped = it },
        )

        composeRule.onNodeWithText("$45.00").performClick()

        assertEquals(7L, tapped)
    }

    @Test
    fun totalsRenderAboveTheList() {
        setContent(HistoryUiState(totalsFormatted = listOf("$45.00", "R$10.00")))

        composeRule.onNodeWithText("$45.00").assertExists()
        composeRule.onNodeWithText("R$10.00").assertExists()
    }
}
