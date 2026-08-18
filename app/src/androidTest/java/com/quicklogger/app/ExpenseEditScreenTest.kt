package com.quicklogger.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.presentation.expenseedit.ExpenseEditEvent
import com.quicklogger.app.presentation.expenseedit.ExpenseEditScreenContent
import com.quicklogger.app.presentation.expenseedit.ExpenseEditUiState
import com.quicklogger.app.presentation.theme.QuickLoggerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)

    private fun setContent(
        state: ExpenseEditUiState,
        onEvent: (ExpenseEditEvent) -> Unit = {},
        receiptFile: java.io.File? = null,
    ) = composeRule.setContent {
        QuickLoggerTheme {
            ExpenseEditScreenContent(
                uiState = state,
                onEvent = onEvent,
                onNavigateUp = {},
                receiptFile = receiptFile,
            )
        }
    }

    @Test
    fun notFoundShowsAMessageInsteadOfTheForm() {
        setContent(ExpenseEditUiState(isLoading = false, notFound = true))

        composeRule.onNodeWithText("This expense no longer exists.").assertExists()
    }

    @Test
    fun theLoadedAmountAndCategoryRender() {
        setContent(
            ExpenseEditUiState(
                isLoading = false,
                amountFormatted = "$45.00",
                categories = listOf(food),
                selectedCategoryId = food.id,
            ),
        )

        composeRule.onNodeWithText("$45.00").assertExists()
        composeRule.onNodeWithText("Food").assertExists()
    }

    @Test
    fun saveIsEnabledWithAValidForm() {
        setContent(
            ExpenseEditUiState(
                isLoading = false,
                amountDigits = "4500",
                amountFormatted = "$45.00",
                categories = listOf(food),
                selectedCategoryId = food.id,
            ),
        )

        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun saveIsDisabledWithAZeroAmount() {
        setContent(
            ExpenseEditUiState(
                isLoading = false,
                amountDigits = "0",
                amountFormatted = "$0.00",
                categories = listOf(food),
                selectedCategoryId = food.id,
            ),
        )

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun deleteOpensAConfirmationBeforeEmittingDelete() {
        val events = mutableListOf<ExpenseEditEvent>()
        setContent(
            ExpenseEditUiState(
                isLoading = false,
                categories = listOf(food),
                selectedCategoryId = food.id,
            ),
            onEvent = { events += it },
        )

        composeRule.onNodeWithContentDescription("Delete").performClick()
        assertEquals(emptyList<ExpenseEditEvent>(), events)

        composeRule.onNodeWithText("Delete this expense?").assertExists()
    }

    @Test
    fun confirmingDeleteEmitsTheEvent() {
        val events = mutableListOf<ExpenseEditEvent>()
        setContent(
            ExpenseEditUiState(
                isLoading = false,
                categories = listOf(food),
                selectedCategoryId = food.id,
            ),
            onEvent = { events += it },
        )
        composeRule.onNodeWithContentDescription("Delete").performClick()

        // The trigger is an icon (content description); the dialog's confirm action
        // is the only node with visible text "Delete".
        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(listOf(ExpenseEditEvent.Delete), events)
    }

    @Test
    fun tappingAnAttachedReceiptOpensAFullSizeView() {
        setContent(
            ExpenseEditUiState(
                isLoading = false,
                categories = listOf(food),
                selectedCategoryId = food.id,
                receiptRelativePath = "abc.jpg",
            ),
            receiptFile = java.io.File("/does/not/need/to/exist.jpg"),
        )

        composeRule.onNodeWithContentDescription("Close receipt").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Receipt attached").performClick()
        composeRule.onNodeWithContentDescription("Close receipt").assertExists()
        composeRule.onNodeWithContentDescription("Close receipt").performClick()
        composeRule.onNodeWithContentDescription("Close receipt").assertDoesNotExist()
    }
}
