package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.usecase.CreateCategory
import com.quicklogger.app.domain.usecase.CreateReceiptDraft
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.FormatExpenseShareText
import com.quicklogger.app.domain.usecase.ImportReceipt
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ReceiptHasContent
import com.quicklogger.app.domain.usecase.SaveExpense
import com.quicklogger.app.presentation.log.LogEvent
import com.quicklogger.app.presentation.log.LogViewModel
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Provider

/**
 * Exhaustive receipt-state-machine coverage moved to `ReceiptAttachmentControllerTest`
 * in sprint 4. What's left here is specific to `LogViewModel`: that it wires the
 * controller into its own `UiState` correctly, and that save/category-creation behave.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelReceiptTest {
    private val dispatcher = StandardTestDispatcher()
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)

    private lateinit var receipts: FakeReceiptStore
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var categories: FakeCategoryRepository

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): LogViewModel {
        receipts = FakeReceiptStore()
        expenses = FakeExpenseRepository()
        categories = FakeCategoryRepository(listOf(food))
        return LogViewModel(
            observeCategories = ObserveCategories(categories),
            saveExpense = SaveExpense(
                expenses,
                categories,
                Clock.fixed(Instant.parse("2026-08-17T17:32:00Z"), ZoneOffset.UTC),
            ),
            lastCategoryStore = FakeLastCategoryStore(),
            createCategory = CreateCategory(categories),
            receiptAttachment = ReceiptAttachmentController(
                CreateReceiptDraft(receipts),
                ImportReceipt(receipts),
                DeleteReceipt(receipts),
                ReceiptHasContent(receipts),
            ),
            formatExpenseShareText = FormatExpenseShareText(),
            localeProvider = Provider { Locale.US },
            zoneProvider = Provider { ZoneOffset.UTC },
        )
    }

    @Test
    fun theControllersAttachedReceiptReachesUiState() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()

        val attached = viewModel.uiState.value.receiptRelativePath
        assertTrue(receipts.files.containsKey(attached))
    }

    @Test
    fun savingPersistsTheReceiptPathOnTheExpense() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()
        val attached = viewModel.uiState.value.receiptRelativePath!!
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertEquals(attached, expenses.inserted.single().receiptRelativePath)
    }

    @Test
    fun savingClearsTheReceiptFromTheFormWithoutDeletingTheFile() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()
        val attached = viewModel.uiState.value.receiptRelativePath!!
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
        assertTrue("the saved expense still owns its file", receipts.files.containsKey(attached))
        assertTrue(receipts.deleted.isEmpty())
    }

    @Test
    fun savingWithNoReceiptStillWrites() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertNull(expenses.inserted.single().receiptRelativePath)
    }

    @Test
    fun saveIsBlockedWhileACopyIsStillRunning() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        // No advanceUntilIdle: the import is still in flight.

        assertFalse(viewModel.uiState.value.canSave)
    }

    // --- category creation from the + chip ---

    @Test
    fun creatingACategoryAddsItAndAutoSelectsIt() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.CreateCategoryRequested("Groceries"))
        advanceUntilIdle()

        val created = viewModel.uiState.value.categories.single { it.name == "Groceries" }
        assertEquals(created.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun aDuplicateCategoryNameSurfacesAnErrorAndChangesNothing() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.CreateCategoryRequested("food"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.categories.size)
        assertTrue(viewModel.uiState.value.categoryError != null)
        assertEquals(food.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun dismissingTheCategoryErrorClearsIt() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CreateCategoryRequested("food"))
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.DismissCategoryError)

        assertNull(viewModel.uiState.value.categoryError)
    }
}
