package com.quicklogger.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.presentation.log.LogEvent
import com.quicklogger.app.presentation.log.LogScreenContent
import com.quicklogger.app.presentation.log.LogUiState
import com.quicklogger.app.presentation.theme.QuickLoggerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke coverage only (ARCHITECTURE §12): the stateless content composable driven by
 * hand-built state. Behavior lives in `LogViewModelTest` on the JVM.
 */
@RunWith(AndroidJUnit4::class)
class LogScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val transport = Category(id = 2L, name = "Transport", sortOrder = 1, isProtected = false)

    private fun setContent(
        state: LogUiState,
        onEvent: (LogEvent) -> Unit = {},
    ) = composeRule.setContent {
        QuickLoggerTheme {
            LogScreenContent(uiState = state, onEvent = onEvent, onOpenHistory = {})
        }
    }

    @Test
    fun saveIsDisabledWhenTheAmountIsEmpty() {
        setContent(LogUiState(categories = listOf(food), selectedCategoryId = food.id))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveIsEnabledOnceAPositiveAmountIsTyped() {
        setContent(
            LogUiState(
                amountDigits = "4500",
                amountFormatted = "$45.00",
                categories = listOf(food),
                selectedCategoryId = food.id,
            ),
        )

        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun theAmountFieldTakesFocusOnLaunch() {
        setContent(LogUiState(categories = listOf(food), selectedCategoryId = food.id))

        // The editable field is the only node that accepts text input.
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun chipsRenderEveryCategory() {
        setContent(
            LogUiState(
                categories = listOf(food, transport),
                selectedCategoryId = food.id,
            ),
        )

        composeRule.onNodeWithText("Food").assertExists()
        composeRule.onNodeWithText("Transport").assertExists()
    }

    @Test
    fun tappingAnUnselectedChipEmitsASelection() {
        val events = mutableListOf<LogEvent>()
        setContent(
            LogUiState(categories = listOf(food, transport), selectedCategoryId = food.id),
            onEvent = { events += it },
        )

        composeRule.onNodeWithText("Transport").performClick()

        assertEquals(listOf(LogEvent.CategorySelected(transport.id)), events)
    }

    @Test
    fun tappingTheSelectedChipDoesNothing() {
        val events = mutableListOf<LogEvent>()
        setContent(
            LogUiState(categories = listOf(food, transport), selectedCategoryId = food.id),
            onEvent = { events += it },
        )

        composeRule.onNodeWithText("Food").performClick()

        assertEquals(emptyList<LogEvent>(), events)
    }
}
