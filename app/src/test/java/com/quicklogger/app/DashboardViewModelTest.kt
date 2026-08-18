package com.quicklogger.app

import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.usecase.BuildExpensesCsv
import com.quicklogger.app.domain.usecase.BuildPeriodSummary
import com.quicklogger.app.domain.usecase.ClearBudgetTarget
import com.quicklogger.app.domain.usecase.ExportExpensesCsv
import com.quicklogger.app.domain.usecase.ObserveBudgetTargets
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ObserveExpensesInRange
import com.quicklogger.app.domain.usecase.SetBudgetTarget
import com.quicklogger.app.presentation.dashboard.DashboardEvent
import com.quicklogger.app.presentation.dashboard.DashboardUiEvent
import com.quicklogger.app.presentation.dashboard.DashboardViewModel
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
class DashboardViewModelTest {
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

    private fun viewModel(
        expenses: List<Expense>,
        categories: List<Category> = listOf(food, transport),
        budgetTargets: List<BudgetTarget> = emptyList(),
    ): DashboardViewModel = viewModelWithRepo(expenses, categories, budgetTargets).first

    private fun viewModelWithRepo(
        expenses: List<Expense>,
        categories: List<Category> = listOf(food, transport),
        budgetTargets: List<BudgetTarget> = emptyList(),
        clock: Clock = Clock.fixed(now, ZoneOffset.UTC),
    ): Pair<DashboardViewModel, FakeExpenseRepository> {
        val expenseRepo = FakeExpenseRepository()
        val categoryRepo = FakeCategoryRepository(categories)
        val budgetTargetRepo = FakeBudgetTargetRepository(budgetTargets)
        expenses.forEach { runBlockingInsert(expenseRepo, it) }
        csvExportStore = FakeCsvExportStore()
        val viewModel = DashboardViewModel(
            ObserveExpensesInRange(expenseRepo),
            ObserveCategories(categoryRepo),
            ObserveBudgetTargets(budgetTargetRepo),
            BuildPeriodSummary(),
            ExportExpensesCsv(BuildExpensesCsv(), csvExportStore, clock),
            SetBudgetTarget(budgetTargetRepo),
            ClearBudgetTarget(budgetTargetRepo),
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
    private suspend inline fun <reified T : DashboardUiEvent> nextEventOrNull(viewModel: DashboardViewModel): T? =
        withTimeoutOrNull(1) { viewModel.uiEvents.filterIsInstance<T>().first() }

    private fun runBlockingInsert(repo: FakeExpenseRepository, expense: Expense) =
        kotlinx.coroutines.runBlocking { repo.insert(expense) }

    /**
     * `uiState` is `stateIn(..., WhileSubscribed(5_000))` per ARCHITECTURE §5 rule 2:
     * the upstream `combine` never runs — `.value` stays the initial default forever
     * — until something actually subscribes.
     */
    private fun TestScope.keepUiStateAlive(viewModel: DashboardViewModel) {
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

        viewModel.onEvent(DashboardEvent.PeriodSelected(Period.WEEK))
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

        viewModel.onEvent(DashboardEvent.PeriodSelected(Period.MONTH))
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

        viewModel.onEvent(DashboardEvent.SharePeriodText)
        advanceUntilIdle()

        val share = requireNotNull(nextEventOrNull<DashboardUiEvent.ShareText>(viewModel))
        assertTrue(share.text.contains("Transport"))
    }

    @Test
    fun exportCsvWritesTheVisibleRowsAndFiresAShareCsvEventNamedForToday() = runTest {
        val today = expense(1L, now)
        val (viewModel, _) = viewModelWithRepo(listOf(today))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.ExportCsv)
        advanceUntilIdle()

        val share = requireNotNull(nextEventOrNull<DashboardUiEvent.ShareCsv>(viewModel))
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
        viewModel.onEvent(DashboardEvent.PeriodSelected(Period.MONTH))
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.ExportCsv)
        advanceUntilIdle()

        val share = requireNotNull(nextEventOrNull<DashboardUiEvent.ShareCsv>(viewModel))
        assertEquals("quicklogger-2026-08-18.csv", share.fileName)
    }

    // --- budget overview (sprint 7, ARCHITECTURE §6.5 / DESIGN §4.2) ---

    @Test
    fun noOverviewWhenNoTargetsAndNoSpend() = runTest {
        val viewModel = viewModel(emptyList())
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.overview.isEmpty)
    }

    @Test
    fun anOverallTargetProducesAMeterEvenWithNoSpend() = runTest {
        val viewModel = viewModel(emptyList(), budgetTargets = listOf(BudgetTarget(null, Money(50_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        val meter = requireNotNull(viewModel.uiState.value.overview.meter)
        assertFalse(meter.isOver)
        assertEquals("$500.00", meter.targetFormatted)
    }

    @Test
    fun theMeterReflectsThisMonthsSpendRegardlessOfThePeriodChip() = runTest {
        val thisMonth = expense(1L, Instant.parse("2026-08-02T10:00:00Z"), minor = 10_000)
        val viewModel = viewModel(listOf(thisMonth), budgetTargets = listOf(BudgetTarget(null, Money(50_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()
        // The DAY chip would exclude this expense from the list, but not from the overview.
        viewModel.onEvent(DashboardEvent.PeriodSelected(Period.DAY))
        advanceUntilIdle()

        val meter = requireNotNull(viewModel.uiState.value.overview.meter)
        assertEquals("$400.00", meter.remainingFormatted)
    }

    @Test
    fun spendingPastTheOverallTargetMarksTheMeterOver() = runTest {
        val thisMonth = expense(1L, now, minor = 60_000)
        val viewModel = viewModel(listOf(thisMonth), budgetTargets = listOf(BudgetTarget(null, Money(50_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        val meter = requireNotNull(viewModel.uiState.value.overview.meter)
        assertTrue(meter.isOver)
    }

    @Test
    fun aBarAppearsForACategoryWithSpendButNoTarget() = runTest {
        val thisMonth = expense(1L, now, categoryId = transport.id, minor = 2_000)
        val viewModel = viewModel(listOf(thisMonth))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        val bar = viewModel.uiState.value.overview.bars.single()
        assertEquals("Transport", bar.categoryName)
        assertNull(bar.targetFraction)
        assertFalse(bar.isOver)
    }

    @Test
    fun aBarAppearsForACategoryWithATargetButNoSpend() = runTest {
        val viewModel = viewModel(emptyList(), budgetTargets = listOf(BudgetTarget(food.id, Money(5_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        val bar = viewModel.uiState.value.overview.bars.single()
        assertEquals("Food", bar.categoryName)
        assertEquals(0f, bar.fillFraction)
    }

    @Test
    fun aCategoryWithNeitherSpendNorTargetHasNoBar() = runTest {
        val thisMonth = expense(1L, now, categoryId = food.id)
        val viewModel = viewModel(listOf(thisMonth), budgetTargets = listOf(BudgetTarget(food.id, Money(10_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        assertEquals(listOf("Food"), viewModel.uiState.value.overview.bars.map { it.categoryName })
    }

    @Test
    fun editingTheOverallTargetOpensADialogWithNoTitle() = runTest {
        val viewModel = viewModel(emptyList())
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.EditBudgetTarget(null))
        advanceUntilIdle()

        val dialog = requireNotNull(viewModel.uiState.value.targetDialog)
        assertNull(dialog.categoryId)
        assertNull(dialog.categoryName)
    }

    @Test
    fun editingACategoryTargetPrefillsItsExistingAmount() = runTest {
        val viewModel = viewModel(emptyList(), budgetTargets = listOf(BudgetTarget(food.id, Money(5_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.EditBudgetTarget(food.id))
        advanceUntilIdle()

        val dialog = requireNotNull(viewModel.uiState.value.targetDialog)
        assertEquals("Food", dialog.categoryName)
        assertEquals("5000", dialog.amountDigits)
    }

    @Test
    fun confirmingAPositiveAmountSetsTheTargetAndDismissesTheDialog() = runTest {
        val viewModel = viewModel(emptyList())
        keepUiStateAlive(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(DashboardEvent.EditBudgetTarget(food.id))
        viewModel.onEvent(DashboardEvent.BudgetTargetAmountChanged("2500"))

        viewModel.onEvent(DashboardEvent.ConfirmBudgetTarget)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.targetDialog)
        val bar = viewModel.uiState.value.overview.bars.single()
        assertEquals("Food", bar.categoryName)
    }

    @Test
    fun confirmingAnEmptyAmountClearsAnExistingTarget() = runTest {
        val viewModel = viewModel(emptyList(), budgetTargets = listOf(BudgetTarget(food.id, Money(5_000, "USD"))))
        keepUiStateAlive(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(DashboardEvent.EditBudgetTarget(food.id))
        viewModel.onEvent(DashboardEvent.BudgetTargetAmountChanged(""))

        viewModel.onEvent(DashboardEvent.ConfirmBudgetTarget)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.overview.isEmpty)
    }

    @Test
    fun dismissingTheDialogWithoutConfirmingChangesNothing() = runTest {
        val viewModel = viewModel(emptyList())
        keepUiStateAlive(viewModel)
        advanceUntilIdle()
        viewModel.onEvent(DashboardEvent.EditBudgetTarget(food.id))
        viewModel.onEvent(DashboardEvent.BudgetTargetAmountChanged("2500"))

        viewModel.onEvent(DashboardEvent.DismissBudgetTargetDialog)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.targetDialog)
        assertTrue(viewModel.uiState.value.overview.isEmpty)
    }
}
