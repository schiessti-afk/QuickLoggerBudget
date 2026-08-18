package com.quicklogger.app

import androidx.lifecycle.SavedStateHandle
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.usecase.CreateReceiptDraft
import com.quicklogger.app.domain.usecase.DeleteExpense
import com.quicklogger.app.domain.usecase.DeleteReceipt
import com.quicklogger.app.domain.usecase.ImportReceipt
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ReceiptHasContent
import com.quicklogger.app.domain.usecase.UpdateExpense
import com.quicklogger.app.presentation.expenseedit.ExpenseEditEvent
import com.quicklogger.app.presentation.expenseedit.ExpenseEditUiEvent
import com.quicklogger.app.presentation.expenseedit.ExpenseEditViewModel
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val transport = Category(id = 2L, name = "Transport", sortOrder = 1, isProtected = false)
    private val savedAt = Instant.parse("2026-08-10T12:00:00Z")
    private val editedAt = Instant.parse("2026-08-17T17:32:00Z")

    private lateinit var expenses: FakeExpenseRepository
    private lateinit var receipts: FakeReceiptStore

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun stored(id: Long = 1L, receipt: String? = "abc.jpg") = Expense(
        id = id,
        amount = Money(4500, "BRL"),
        categoryId = food.id,
        occurredAt = savedAt,
        receiptRelativePath = receipt,
        createdAt = savedAt,
        updatedAt = savedAt,
    )

    private fun viewModel(id: Long = 1L, seedExpenses: List<Expense> = emptyList()): ExpenseEditViewModel {
        expenses = FakeExpenseRepository()
        receipts = FakeReceiptStore()
        val categories = FakeCategoryRepository(listOf(food, transport))
        kotlinx.coroutines.runBlocking { seedExpenses.forEach { expenses.insert(it) } }

        return ExpenseEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ExpenseEditViewModel.ARG_ID to id)),
            expenses = expenses,
            observeCategories = ObserveCategories(categories),
            updateExpense = UpdateExpense(expenses, categories, Clock.fixed(editedAt, ZoneOffset.UTC)),
            deleteExpense = DeleteExpense(expenses, DeleteReceipt(receipts)),
            receiptAttachment = ReceiptAttachmentController(
                CreateReceiptDraft(receipts),
                ImportReceipt(receipts),
                DeleteReceipt(receipts),
                ReceiptHasContent(receipts),
            ),
            zoneProvider = Provider { ZoneOffset.UTC },
            localeProvider = Provider { Locale.US },
        )
    }

    private fun TestScope.collectUiEvents(viewModel: ExpenseEditViewModel): List<ExpenseEditUiEvent> {
        val collected = mutableListOf<ExpenseEditUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { collected += it }
        }
        return collected
    }

    // --- load ---

    @Test
    fun loadsTheExpenseIntoTheForm() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("4500", state.amountDigits)
        assertEquals("BRL", state.currencyCode)
        assertEquals(food.id, state.selectedCategoryId)
        assertEquals(savedAt, state.occurredAt)
        assertEquals("abc.jpg", state.receiptRelativePath)
        assertTrue(receipts.files.isEmpty()) // seeding does not touch the receipt store
    }

    @Test
    fun aMissingExpenseSetsNotFound() = runTest {
        val viewModel = viewModel(id = 999L, seedExpenses = emptyList())

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notFound)
        assertEquals(false, viewModel.uiState.value.canSave)
    }

    // --- edit fields ---

    @Test
    fun changingTheAmountKeepsTheOriginalCurrency() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.AmountChanged("999"))

        assertEquals("999", viewModel.uiState.value.amountDigits)
        assertEquals("BRL", viewModel.uiState.value.currencyCode)
    }

    @Test
    fun changingTheOccurredAtUpdatesTheFormattedString() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        advanceUntilIdle()
        val before = viewModel.uiState.value.occurredAtFormatted

        val newInstant = Instant.parse("2026-01-01T00:00:00Z")
        viewModel.onEvent(ExpenseEditEvent.OccurredAtChanged(newInstant))

        assertEquals(newInstant, viewModel.uiState.value.occurredAt)
        assertTrue(before != viewModel.uiState.value.occurredAtFormatted)
    }

    // --- save ---

    @Test
    fun savingPersistsAmountCategoryReceiptAndOccurredAt() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        advanceUntilIdle()
        val newOccurredAt = Instant.parse("2026-08-05T09:00:00Z")

        viewModel.onEvent(ExpenseEditEvent.AmountChanged("999"))
        viewModel.onEvent(ExpenseEditEvent.CategorySelected(transport.id))
        viewModel.onEvent(ExpenseEditEvent.OccurredAtChanged(newOccurredAt))
        viewModel.onEvent(ExpenseEditEvent.Save)
        advanceUntilIdle()

        val saved = expenses.inserted.single { it.id == 1L }
        assertEquals(999L, saved.amount.minor)
        assertEquals(transport.id, saved.categoryId)
        assertEquals(newOccurredAt, saved.occurredAt)
    }

    @Test
    fun savingEmitsNavigateBack() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.Save)
        advanceUntilIdle()

        assertTrue(events.contains(ExpenseEditUiEvent.NavigateBack))
    }

    @Test
    fun savingAZeroAmountFailsAndDoesNotNavigate() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.AmountChanged("0"))
        viewModel.onEvent(ExpenseEditEvent.Save)
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertEquals(4500L, expenses.inserted.single().amount.minor)
    }

    @Test
    fun savingKeepsTheAttachedReceiptWithoutDeletingIt() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.Save)
        advanceUntilIdle()

        assertTrue(receipts.deleted.isEmpty())
    }

    // --- delete ---

    @Test
    fun deletingRemovesTheRowAndItsReceiptFile() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        receipts.writeBytes("abc.jpg")
        val events = collectUiEvents(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.Delete)
        advanceUntilIdle()

        assertEquals(listOf(1L), expenses.deletedIds)
        assertTrue(receipts.deleted.contains("abc.jpg"))
        assertTrue(events.contains(ExpenseEditUiEvent.NavigateBack))
    }

    @Test
    fun deletingAnExpenseWithNoReceiptDoesNotTouchReceiptStorage() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored(receipt = null)))
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.Delete)
        advanceUntilIdle()

        assertTrue(receipts.deleted.isEmpty())
    }

    // --- receipts (thin: the state machine itself is covered by ReceiptAttachmentControllerTest) ---

    @Test
    fun replacingTheReceiptReachesUiState() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.ReceiptPicked("content://new"))
        advanceUntilIdle()

        val attached = viewModel.uiState.value.receiptRelativePath
        assertTrue(attached != "abc.jpg")
        assertTrue(receipts.deleted.contains("abc.jpg"))
    }

    @Test
    fun removingTheReceiptClearsIt() = runTest {
        val viewModel = viewModel(seedExpenses = listOf(stored()))
        advanceUntilIdle()

        viewModel.onEvent(ExpenseEditEvent.RemoveReceipt)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptRelativePath)
    }
}
