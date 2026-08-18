package com.quicklogger.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.presentation.log.LogEvent
import com.quicklogger.app.presentation.log.LogScreenContent
import com.quicklogger.app.presentation.log.LogUiState
import com.quicklogger.app.presentation.theme.QuickLoggerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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
        receiptFile: File? = null,
    ) = composeRule.setContent {
        QuickLoggerTheme {
            LogScreenContent(
                uiState = state,
                onEvent = onEvent,
                onOpenHistory = {},
                receiptFile = receiptFile,
            )
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

    // --- receipts (sprint 3) ---

    @Test
    fun bothReceiptActionsShowWhenNothingIsAttached() {
        setContent(LogUiState(categories = listOf(food), selectedCategoryId = food.id))

        composeRule.onNodeWithText("Take photo").assertExists()
        composeRule.onNodeWithText("Choose image").assertExists()
    }

    @Test
    fun tappingTakePhotoAsksTheViewModelForADraft() {
        val events = mutableListOf<LogEvent>()
        setContent(
            LogUiState(categories = listOf(food), selectedCategoryId = food.id),
            onEvent = { events += it },
        )

        composeRule.onNodeWithText("Take photo").performClick()

        assertEquals(listOf(LogEvent.CaptureReceipt), events)
    }

    @Test
    fun anAttachedReceiptReplacesTheActionsWithAThumbnailAndRemove() {
        setContent(
            LogUiState(
                categories = listOf(food),
                selectedCategoryId = food.id,
                receiptRelativePath = "abc.jpg",
            ),
            receiptFile = File("/does/not/need/to/exist.jpg"),
        )

        composeRule.onNodeWithText("Take photo").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Remove receipt").assertExists()
    }

    @Test
    fun removingAnAttachedReceiptEmitsRemove() {
        val events = mutableListOf<LogEvent>()
        setContent(
            LogUiState(
                categories = listOf(food),
                selectedCategoryId = food.id,
                receiptRelativePath = "abc.jpg",
            ),
            onEvent = { events += it },
            receiptFile = File("/does/not/need/to/exist.jpg"),
        )

        composeRule.onNodeWithContentDescription("Remove receipt").performClick()

        assertEquals(listOf(LogEvent.RemoveReceipt), events)
    }

    @Test
    fun anOversizedPickShowsItsError() {
        setContent(
            LogUiState(
                categories = listOf(food),
                selectedCategoryId = food.id,
                receiptError = ReceiptError.TooLarge,
            ),
        )

        composeRule.onNodeWithText("That image is over 10 MB. Choose a smaller one.").assertExists()
    }

    @Test
    fun saveIsBlockedWhileAReceiptIsStillCopying() {
        setContent(
            LogUiState(
                amountDigits = "4500",
                amountFormatted = "$45.00",
                categories = listOf(food),
                selectedCategoryId = food.id,
                isAttachingReceipt = true,
            ),
        )

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    // --- category creation (sprint 4) ---

    @Test
    fun tappingThePlusChipOpensTheCreateDialog() {
        setContent(LogUiState(categories = listOf(food), selectedCategoryId = food.id))

        composeRule.onNodeWithText("+").performClick()

        composeRule.onNodeWithText("New category").assertExists()
    }

    @Test
    fun submittingANameEmitsCreateCategoryRequested() {
        val events = mutableListOf<LogEvent>()
        setContent(
            LogUiState(categories = listOf(food), selectedCategoryId = food.id),
            onEvent = { events += it },
        )
        composeRule.onNodeWithText("+").performClick()

        composeRule.onNodeWithText("Name").performTextInput("Groceries")
        composeRule.onNodeWithText("Create").performClick()

        assertEquals(listOf(LogEvent.CreateCategoryRequested("Groceries")), events)
    }

    @Test
    fun aCategoryErrorShowsBelowTheChips() {
        setContent(
            LogUiState(
                categories = listOf(food),
                selectedCategoryId = food.id,
                categoryError = CategoryError.DuplicateName,
            ),
        )

        composeRule.onNodeWithText("A category with this name already exists.").assertExists()
    }
}
