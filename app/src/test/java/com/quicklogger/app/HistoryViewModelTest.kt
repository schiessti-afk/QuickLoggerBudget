package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.usecase.BuildExpensesCsv
import com.quicklogger.app.domain.usecase.BuildPeriodSummary
import com.quicklogger.app.domain.usecase.ExportExpensesCsv
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ObserveExpensesInRange
import com.quicklogger.app.presentation.history.HistoryEvent
import com.quicklogger.app.presentation.history.HistoryUiEvent
import com.quicklogger.app.presentation.history.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val transport = Category(id = 2L, name = "Transport", sortOrder = 1, isProtected = false)
    // A Tuesday: today, this week (Monday 8/17-8/18), and this month (8/1-8/18) all differ.
    private val now = Instant.parse("2026-08-18T14:00:00Z")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun expense(id: Long, occurredAt: Instant, categoryId: Long = food.id, minor: Long = 4500, currency: String = "USD", receipt: String? = null) =
        Expense(id, Money(minor, currency), categoryId, occurredAt, receipt, occurredAt, occurredAt)

    private lateinit var csvExportStore: FakeCsvExportStore

    private fun viewModel(expenses: List<Expense>, categories: List<Category> = listOf(food, transport)): HistoryViewModel =
        viewModelWithRepo(expenses, categories).first

    private fun viewModelWithRepo(
        expenses: List<Expense>,
        categories: List<Category> = listOf(food, transport),
        clock: Clock = Clock.fixed(now, ZoneOffset.UTC),
    ): Pair<HistoryViewModel, FakeExpenseRepository> {
        val expenseRepo = FakeExpenseRepository()
        val categoryRepo = FakeCategoryRepository(categories)
        expenses.forEach { runBlockingInsert(expenseRepo, it) }
        csvExportStore = FakeCsvExportStore()
        val viewModel = HistoryViewModel(
            ObserveExpensesInRange(expenseRepo),
            ObserveCategories(categoryRepo),
            BuildPeriodSummary(),
            ExportExpensesCsv(BuildExpensesCsv(), csvExportStore, clock),
            clock,
            Provider { ZoneOffset.UTC },
            Provider { Locale.US },
        )
        return viewModel to expenseRepo
    }

    /**
     * Reads the next buffered event directly rather than through a long-lived
     * background collector — by the time this is called, a fired event is already
     * sitting in the channel's buffer, so `first()` returns without suspending.
     */
    private suspend inline fun <reified T : HistoryUiEvent> nextEventOrNull(viewModel: HistoryViewModel): T? =
        withTimeoutOrNull(1) { viewModel.uiEvents.filterIsInstance<T>().first() }

    private fun runBlockingInsert(repo: FakeExpenseRepository, expense: Expense) =
        kotlinx.coroutines.runBlocking { repo.insert(expense) }

    /**
     * `uiState` is `stateIn(..., WhileSubscribed(5_000))` per ARCHITECTURE §5 rule 2:
     * the upstream `combine` never runs — `.value` stays the initial default forever
     * — until something actually subscribes.
     */
    private fun TestScope.keepUiStateAlive(viewModel: HistoryViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun defaultsToTodaysExpenses() = runTest {
        val today = expense(1L, Instant.parse("2026-08-18T10:00:00Z"))
        val yesterday = expense(2L, Instant.parse("2026-08-17T10:00:00Z"))
        val viewModel = viewModel(listOf(today, yesterday))
        keepUiStateAlive(viewModel)

        advanceUntilIdle()

        assertEquals(listOf(today.id), viewModel.uiState.value.rows.map { it.id })
    }

    @Test
    fun switchingToWeekIncludesTheWholeCurrentWeek() = runTest {
        val monday = expense(1L, Instant.parse("2026-08-17T10:00:00Z"))
        val lastWeek = expense(2L, Instant.parse("2026-08-10T10:00:00Z"))
        val viewModel = viewModel(listOf(monday, lastWeek))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.PeriodSelected(Period.WEEK))
        advanceUntilIdle()

        assertEquals(listOf(monday.id), viewModel.uiState.value.rows.map { it.id })
        assertEquals(Period.WEEK, viewModel.uiState.value.period)
    }

    @Test
    fun switchingToMonthIncludesTheWholeCurrentMonth() = runTest {
        val earlyThisMonth = expense(1L, Instant.parse("2026-08-02T10:00:00Z"))
        val lastMonth = expense(2L, Instant.parse("2026-07-31T10:00:00Z"))
        val viewModel = viewModel(listOf(earlyThisMonth, lastMonth))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.PeriodSelected(Period.MONTH))
        advanceUntilIdle()

        assertEquals(listOf(earlyThisMonth.id), viewModel.uiState.value.rows.map { it.id })
    }

    @Test
    fun rowsCarryTheCategoryNameAndReceiptIndicator() = runTest {
        val withReceipt = expense(1L, now, categoryId = transport.id, receipt = "abc.jpg")
        val viewModel = viewModel(listOf(withReceipt))
        keepUiStateAlive(viewModel)

        advanceUntilIdle()

        val row = viewModel.uiState.value.rows.single()
        assertEquals("Transport", row.categoryName)
        assertTrue(row.hasReceipt)
    }

    @Test
    fun totalsAreOneLinePerCurrency() = runTest {
        val brl = expense(1L, now, minor = 4500, currency = "BRL")
        val jpy = expense(2L, now, minor = 1000, currency = "JPY")
        val viewModel = viewModel(listOf(brl, jpy))
        keepUiStateAlive(viewModel)

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.totalsFormatted.size)
    }

    @Test
    fun emptyPeriodHasNoRowsAndIsEmpty() = runTest {
        val viewModel = viewModel(emptyList())
        keepUiStateAlive(viewModel)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun newestFirstOrderIsPreserved() = runTest {
        val earlier = expense(1L, Instant.parse("2026-08-18T08:00:00Z"))
        val later = expense(2L, Instant.parse("2026-08-18T12:00:00Z"))
        val viewModel = viewModel(listOf(earlier, later))
        keepUiStateAlive(viewModel)

        advanceUntilIdle()

        assertEquals(listOf(later.id, earlier.id), viewModel.uiState.value.rows.map { it.id })
    }

    @Test
    fun theListUpdatesWhenANewExpenseIsLoggedWithoutRestartingTheViewModel() = runTest {
        val existing = expense(1L, now)
        val (viewModel, repo) = viewModelWithRepo(listOf(existing))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()
        assertEquals(listOf(existing.id), viewModel.uiState.value.rows.map { it.id })

        val justLogged = expense(2L, now.plusSeconds(60))
        repo.insert(justLogged)
        advanceUntilIdle()

        assertEquals(
            listOf(justLogged.id, existing.id),
            viewModel.uiState.value.rows.map { it.id },
        )
    }

    // --- share and CSV export (ARCHITECTURE §9.2, §9.3) ---

    @Test
    fun sharePeriodTextFiresAShareEventBuiltFromTheVisibleRows() = runTest {
        val today = expense(1L, now, categoryId = transport.id)
        val viewModel = viewModel(listOf(today))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.SharePeriodText)
        advanceUntilIdle()

        val share = requireNotNull(nextEventOrNull<HistoryUiEvent.ShareText>(viewModel))
        assertTrue(share.text.contains("Transport"))
    }

    @Test
    fun exportCsvWritesTheVisibleRowsAndFiresAShareCsvEventNamedForToday() = runTest {
        val today = expense(1L, now)
        val (viewModel, _) = viewModelWithRepo(listOf(today))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.ExportCsv)
        advanceUntilIdle()

        val share = requireNotNull(nextEventOrNull<HistoryUiEvent.ShareCsv>(viewModel))
        assertEquals("quicklogger-2026-08-18.csv", share.fileName)
        val written = csvExportStore.written.getValue(share.fileName)
        assertTrue(written.contains("occurred_at,amount,currency,category,has_receipt"))
        assertEquals(2, written.trim().lines().size)
    }

    @Test
    fun exportCsvUsesTheExportDateNotThePeriodStartForWeekAndMonth() = runTest {
        val lastWeek = expense(1L, Instant.parse("2026-08-10T10:00:00Z"))
        val (viewModel, _) = viewModelWithRepo(listOf(lastWeek))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(HistoryEvent.PeriodSelected(Period.MONTH))
        advanceUntilIdle()

        viewModel.onEvent(HistoryEvent.ExportCsv)
        advanceUntilIdle()

        val share = requireNotNull(nextEventOrNull<HistoryUiEvent.ShareCsv>(viewModel))
        assertEquals("quicklogger-2026-08-18.csv", share.fileName)
    }
}
