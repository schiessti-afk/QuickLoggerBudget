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
import com.quicklogger.app.presentation.log.LogUiEvent
import com.quicklogger.app.presentation.log.LogViewModel
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
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

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val transport = Category(id = 2L, name = "Transport", sortOrder = 1, isProtected = false)
    private val other = Category(id = 6L, name = "Other", sortOrder = 5, isProtected = true)
    private val seeded = listOf(food, transport, other)

    private lateinit var expenses: FakeExpenseRepository
    private lateinit var lastCategory: FakeLastCategoryStore

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        categories: List<Category> = seeded,
        remembered: Long? = null,
        locale: Locale = Locale.US,
    ): LogViewModel {
        expenses = FakeExpenseRepository()
        lastCategory = FakeLastCategoryStore(remembered)
        val categoryRepository = FakeCategoryRepository(categories)
        val receipts = FakeReceiptStore()
        return LogViewModel(
            observeCategories = ObserveCategories(categoryRepository),
            saveExpense = SaveExpense(
                expenses,
                categoryRepository,
                Clock.fixed(Instant.parse("2026-08-17T17:32:00Z"), ZoneOffset.UTC),
            ),
            lastCategoryStore = lastCategory,
            createCategory = CreateCategory(categoryRepository),
            receiptAttachment = ReceiptAttachmentController(
                CreateReceiptDraft(receipts),
                ImportReceipt(receipts),
                DeleteReceipt(receipts),
                ReceiptHasContent(receipts),
            ),
            formatExpenseShareText = FormatExpenseShareText(),
            localeProvider = Provider { locale },
            zoneProvider = Provider { ZoneOffset.UTC },
        )
    }

    /**
     * Reads the next buffered `Share` event directly, rather than through a
     * long-lived background collector: by the time this is called the event is
     * already sitting in the channel's buffer, so `first()` returns without
     * suspending. `withTimeoutOrNull` on a virtual clock returns `null` instantly
     * when nothing is buffered, so this doubles as "no event fired".
     */
    private suspend fun nextShareEventOrNull(viewModel: LogViewModel): LogUiEvent.Share? =
        withTimeoutOrNull(1) { viewModel.uiEvents.filterIsInstance<LogUiEvent.Share>().first() }

    // --- cold start selection (ARCHITECTURE §6.3) ---

    @Test
    fun coldStartSelectsTheRememberedCategoryWithoutATap() = runTest {
        val viewModel = viewModel(remembered = transport.id)

        advanceUntilIdle()

        assertEquals(transport.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun coldStartFallsBackToLowestSortOrderWhenNothingIsRemembered() = runTest {
        val viewModel = viewModel(remembered = null)

        advanceUntilIdle()

        assertEquals(food.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun coldStartFallsBackWhenTheRememberedCategoryIsGone() = runTest {
        val viewModel = viewModel(remembered = 404L)

        advanceUntilIdle()

        assertEquals(food.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun coldStartSelectsOtherWhenItIsTheOnlyCategoryLeft() = runTest {
        val viewModel = viewModel(categories = listOf(other), remembered = 404L)

        advanceUntilIdle()

        assertEquals(other.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun selectingACategoryPersistsItImmediately() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.CategorySelected(transport.id))
        advanceUntilIdle()

        assertEquals(transport.id, viewModel.uiState.value.selectedCategoryId)
        assertEquals(listOf(transport.id), lastCategory.writes)
    }

    // --- digit buffer ---

    @Test
    fun digitsAreFormattedAsCurrencyAsTheyArrive() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        assertEquals("$45.00", viewModel.uiState.value.amountFormatted)
        assertEquals("4500", viewModel.uiState.value.amountDigits)
    }

    @Test
    fun nonDigitInputIsDiscarded() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged("$4,5abc00"))

        assertEquals("4500", viewModel.uiState.value.amountDigits)
    }

    @Test
    fun leadingZerosAreDropped() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged("00045"))

        assertEquals("45", viewModel.uiState.value.amountDigits)
        assertEquals("$0.45", viewModel.uiState.value.amountFormatted)
    }

    @Test
    fun emptyAmountFormatsToEmptyAndBlocksSave() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged(""))

        assertEquals("", viewModel.uiState.value.amountFormatted)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun zeroAmountBlocksSave() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged("0"))

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun aPositiveAmountWithASelectedCategoryEnablesSave() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        assertTrue(viewModel.uiState.value.canSave)
    }

    // --- save + reset ---

    @Test
    fun saveWritesOneExpenseWithTheLocaleCurrency() = runTest {
        val viewModel = viewModel(locale = Locale.JAPAN)
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertEquals(1, expenses.inserted.size)
        assertEquals(4500L, expenses.inserted.single().amount.minor)
        assertEquals("JPY", expenses.inserted.single().amount.currencyCode)
    }

    @Test
    fun saveClearsTheAmountAndKeepsTheCategory() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CategorySelected(transport.id))
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.amountDigits)
        assertEquals("", state.amountFormatted)
        assertEquals(transport.id, state.selectedCategoryId)
        assertFalse(state.isSaving)
    }

    @Test
    fun theNextExpenseCanBeTypedImmediatelyAfterSaving() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))
        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.AmountChanged("199"))
        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertEquals(2, expenses.inserted.size)
        assertEquals(199L, expenses.inserted.last().amount.minor)
    }

    @Test
    fun saveWithAnEmptyAmountWritesNothing() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertTrue(expenses.inserted.isEmpty())
    }

    @Test
    fun saveWithAZeroAmountWritesNothing() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("0"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertTrue(expenses.inserted.isEmpty())
    }

    @Test
    fun aFailedSaveKeepsTheTypedAmount() = runTest {
        // No categories at all: selection stays null and the defensive domain check
        // is the only thing standing between the tap and a bad row.
        val viewModel = viewModel(categories = emptyList())
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertTrue(expenses.inserted.isEmpty())
        assertEquals("4500", viewModel.uiState.value.amountDigits)
        assertNull(viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun categoriesReachTheState() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(seeded, viewModel.uiState.value.categories)
    }

    // --- Save & Share (ARCHITECTURE §9.1) ---

    @Test
    fun saveAndShareWritesTheExpenseAndFiresAShareEventWithNoReceipt() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.CategorySelected(food.id))
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.SaveAndShare)
        advanceUntilIdle()

        assertEquals(1, expenses.inserted.size)
        val share = requireNotNull(nextShareEventOrNull(viewModel))
        assertNull(share.receiptRelativePath)
        assertTrue(share.text.contains("*QuickLogger*"))
        assertTrue(share.text.contains("Food"))
        assertTrue(share.text.contains("$45.00"))
    }

    @Test
    fun saveAndShareIncludesTheReceiptWhenOneIsAttached() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.ReceiptPicked("content://media/external/images/42"))
        advanceUntilIdle()
        val attached = viewModel.uiState.value.receiptRelativePath
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.SaveAndShare)
        advanceUntilIdle()

        val share = requireNotNull(nextShareEventOrNull(viewModel))
        assertEquals(attached, share.receiptRelativePath)
    }

    @Test
    fun plainSaveDoesNotFireAShareEvent() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.Save)
        advanceUntilIdle()

        assertNull(nextShareEventOrNull(viewModel))
    }

    @Test
    fun saveAndShareResetsTheFormJustLikePlainSave() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogEvent.AmountChanged("4500"))

        viewModel.onEvent(LogEvent.SaveAndShare)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.amountDigits)
        assertFalse(viewModel.uiState.value.isSaving)
    }
}
