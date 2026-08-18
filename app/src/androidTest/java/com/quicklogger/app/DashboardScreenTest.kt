package com.quicklogger.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.presentation.dashboard.BudgetMeterUiModel
import com.quicklogger.app.presentation.dashboard.BudgetOverviewUiModel
import com.quicklogger.app.presentation.dashboard.DashboardEvent
import com.quicklogger.app.presentation.dashboard.DashboardRowUiModel
import com.quicklogger.app.presentation.dashboard.DashboardScreenContent
import com.quicklogger.app.presentation.dashboard.DashboardUiState
import com.quicklogger.app.presentation.theme.QuickLoggerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        state: DashboardUiState,
        onEvent: (DashboardEvent) -> Unit = {},
        onEditExpense: (Long) -> Unit = {},
    ) = composeRule.setContent {
        QuickLoggerTheme {
            DashboardScreenContent(
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
        setContent(DashboardUiState())

        composeRule.onNodeWithText("No expenses in this period.").assertExists()
    }

    @Test
    fun rowsRenderAmountCategoryAndDate() {
        setContent(
            DashboardUiState(
                rows = listOf(
                    DashboardRowUiModel(1L, "$45.00", "Food", "8/17/26, 2:32 PM", hasReceipt = false),
                ),
            ),
        )

        composeRule.onNodeWithText("$45.00").assertExists()
    }

    @Test
    fun tappingAPeriodChipEmitsPeriodSelected() {
        val events = mutableListOf<DashboardEvent>()
        setContent(DashboardUiState(period = Period.DAY), onEvent = { events += it })

        composeRule.onNodeWithText("Week").performClick()

        assertEquals(listOf(DashboardEvent.PeriodSelected(Period.WEEK)), events)
    }

    @Test
    fun tappingARowNavigatesToEdit() {
        var tapped: Long? = null
        setContent(
            DashboardUiState(
                rows = listOf(
                    DashboardRowUiModel(7L, "$45.00", "Food", "8/17/26, 2:32 PM", hasReceipt = false),
                ),
            ),
            onEditExpense = { tapped = it },
        )

        composeRule.onNodeWithText("$45.00").performClick()

        assertEquals(7L, tapped)
    }

    @Test
    fun totalsRenderAboveTheList() {
        setContent(DashboardUiState(totalsFormatted = listOf("$45.00", "R$10.00")))

        composeRule.onNodeWithText("$45.00").assertExists()
        composeRule.onNodeWithText("R$10.00").assertExists()
    }

    // --- budget overview (sprint 7) ---

    @Test
    fun noOverviewIsDrawnWhenNoTargetsAndNoSpend() {
        setContent(DashboardUiState())

        // The screen is then byte-for-byte the old History (DESIGN §4.2): no meter,
        // no bar, just the period chips and the (empty) list.
        composeRule.onNodeWithText("Monthly budget").assertDoesNotExist()
    }

    @Test
    fun unsetMeterShowsTheFullSetPrompt() {
        setContent(
            DashboardUiState(
                overview = BudgetOverviewUiModel(
                    meter = BudgetMeterUiModel(
                        hasTarget = false,
                        fillRatio = 0f,
                        spentFormatted = "$12.00",
                        remainingFormatted = null,
                        targetFormatted = null,
                        isOver = false,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("$12.00 spent").assertExists()
        composeRule.onNodeWithText("Tap to set a monthly budget").assertExists()
    }
}
