package com.quicklogger.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicklogger.app.domain.model.BudgetProgress
import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.ExpenseDateFormatter
import com.quicklogger.app.domain.model.ExpenseTotals
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.model.MoneyFormatter
import com.quicklogger.app.domain.model.Period
import com.quicklogger.app.domain.model.PeriodBounds
import com.quicklogger.app.domain.usecase.BuildPeriodSummary
import com.quicklogger.app.domain.usecase.ClearBudgetTarget
import com.quicklogger.app.domain.usecase.ExportExpensesCsv
import com.quicklogger.app.domain.usecase.ObserveBudgetTargets
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.ObserveExpensesInRange
import com.quicklogger.app.domain.usecase.SetBudgetTarget
import com.quicklogger.app.presentation.log.AmountBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.abs

/**
 * Display formatting happens here, not in Compose (mirrors [com.quicklogger.app.presentation.log.LogViewModel]'s
 * `Provider<Locale>` pattern), so `DashboardScreen` stays a pure function of [uiState].
 *
 * The route was `history` through sprint 6; sprint 7 adds a budget overview above
 * the same list and renames it `dashboard` (ARCHITECTURE §8). The overview is always
 * the *current calendar month*, independent of [DashboardEvent.PeriodSelected] — the
 * period chips still filter only the list and the share/CSV payload, exactly as
 * before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeExpensesInRange: ObserveExpensesInRange,
    observeCategories: ObserveCategories,
    observeBudgetTargets: ObserveBudgetTargets,
    private val buildPeriodSummary: BuildPeriodSummary,
    private val exportExpensesCsv: ExportExpensesCsv,
    private val setBudgetTarget: SetBudgetTarget,
    private val clearBudgetTarget: ClearBudgetTarget,
    private val clock: Clock,
    private val zoneProvider: Provider<ZoneId>,
    private val localeProvider: Provider<Locale>,
) : ViewModel() {
    private val period = MutableStateFlow(Period.DAY)
    private val targetDialog = MutableStateFlow<BudgetTargetDialogUiState?>(null)

    /**
     * The raw data behind the currently rendered [uiState], kept for share/CSV so
     * they build from the same list already on screen rather than re-querying —
     * mirrors [com.quicklogger.app.presentation.expenseedit.ExpenseEditViewModel]'s `loaded` field.
     */
    private var latestExpenses: List<Expense> = emptyList()
    private var latestCategoryNames: Map<Long, String> = emptyMap()
    private var latestBudgetTargets: List<BudgetTarget> = emptyList()

    private val monthExpenses = observeExpensesInRange(PeriodBounds.of(Period.MONTH, today(), zoneProvider.get()))

    /** [Triple] of (categories, budget targets, this month's expenses) — the overview's inputs. */
    private val overviewBasis = combine(observeCategories(), observeBudgetTargets(), monthExpenses) { c, t, e -> Triple(c, t, e) }

    val uiState: StateFlow<DashboardUiState> = combine(
        period,
        period.flatMapLatest { observeExpensesInRange(PeriodBounds.of(it, today(), zoneProvider.get())) },
        overviewBasis,
        targetDialog,
    ) { selectedPeriod, expenses, (categories, targets, expensesThisMonth), dialog ->
        buildState(selectedPeriod, expenses, categories, targets, expensesThisMonth, dialog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _uiEvents = Channel<DashboardUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<DashboardUiEvent> = _uiEvents.receiveAsFlow()

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.PeriodSelected -> period.update { event.period }
            DashboardEvent.SharePeriodText -> sharePeriodText()
            DashboardEvent.ExportCsv -> exportCsv()
            is DashboardEvent.EditBudgetTarget -> openTargetDialog(event.categoryId)
            is DashboardEvent.BudgetTargetAmountChanged -> updateDialogAmount(event.raw)
            DashboardEvent.ConfirmBudgetTarget -> confirmBudgetTarget()
            DashboardEvent.DismissBudgetTargetDialog -> targetDialog.update { null }
        }
    }

    private fun sharePeriodText() {
        viewModelScope.launch {
            val text = buildPeriodSummary(
                period.value,
                latestExpenses,
                latestCategoryNames,
                localeProvider.get(),
                zoneProvider.get(),
            )
            _uiEvents.send(DashboardUiEvent.ShareText(text))
        }
    }

    private fun exportCsv() {
        viewModelScope.launch {
            val fileName = exportExpensesCsv(latestExpenses, latestCategoryNames, zoneProvider.get())
            _uiEvents.send(DashboardUiEvent.ShareCsv(fileName))
        }
    }

    /** Pre-fills the digit buffer from the existing target, if [categoryId] already has one. */
    private fun openTargetDialog(categoryId: Long?) {
        val existingDigits = latestBudgetTargets.firstOrNull { it.categoryId == categoryId }?.amount?.minor?.toString().orEmpty()
        targetDialog.update {
            BudgetTargetDialogUiState(
                categoryId = categoryId,
                categoryName = categoryId?.let { latestCategoryNames[it] },
                amountDigits = existingDigits,
                amountFormatted = MoneyFormatter.formatDigits(existingDigits, currencyCode(), locale()),
            )
        }
    }

    private fun updateDialogAmount(raw: String) {
        val digits = AmountBuffer.sanitize(raw)
        targetDialog.update {
            it?.copy(
                amountDigits = digits,
                amountFormatted = MoneyFormatter.formatDigits(digits, currencyCode(), locale()),
            )
        }
    }

    /** An empty/zero amount clears the target (ARCHITECTURE §6.5); anything else sets it. */
    private fun confirmBudgetTarget() {
        val dialog = targetDialog.value ?: return
        val minor = dialog.amountDigits.toLongOrNull() ?: 0L
        viewModelScope.launch {
            if (minor <= 0L) {
                clearBudgetTarget(dialog.categoryId)
            } else {
                setBudgetTarget(dialog.categoryId, Money(minor, currencyCode()))
            }
        }
        targetDialog.update { null }
    }

    private fun buildState(
        selectedPeriod: Period,
        expenses: List<Expense>,
        categories: List<Category>,
        budgetTargets: List<BudgetTarget>,
        monthExpenses: List<Expense>,
        dialog: BudgetTargetDialogUiState?,
    ): DashboardUiState {
        val locale = localeProvider.get()
        val zone = zoneProvider.get()
        val categoryNames = categories.associate { it.id to it.name }
        latestExpenses = expenses
        latestCategoryNames = categoryNames
        latestBudgetTargets = budgetTargets

        return DashboardUiState(
            period = selectedPeriod,
            rows = expenses.map { expense ->
                DashboardRowUiModel(
                    id = expense.id,
                    amountFormatted = MoneyFormatter.format(expense.amount, locale),
                    categoryName = categoryNames[expense.categoryId].orEmpty(),
                    occurredAtFormatted = ExpenseDateFormatter.format(expense.occurredAt, zone, locale),
                    hasReceipt = expense.receiptRelativePath != null,
                )
            },
            totalsFormatted = ExpenseTotals.byCurrency(expenses).map { MoneyFormatter.format(it, locale) },
            overview = buildOverview(categories, budgetTargets, monthExpenses, locale),
            targetDialog = dialog,
        )
    }

    /** DESIGN §4.2: nothing set and nothing spent draws neither the meter nor any bar. */
    private fun buildOverview(
        categories: List<Category>,
        budgetTargets: List<BudgetTarget>,
        monthExpenses: List<Expense>,
        locale: Locale,
    ): BudgetOverviewUiModel {
        val overallTarget = budgetTargets.firstOrNull { it.categoryId == null }

        val categoryTargetsById = budgetTargets.filter { it.categoryId != null }.associateBy { it.categoryId }
        val expensesByCategory = monthExpenses.groupBy { it.categoryId }
        // A category earns a bar if it has spend this month or a target — spend-descending.
        val categoryIds = (expensesByCategory.keys + categoryTargetsById.keys.filterNotNull()).distinct()

        data class Basis(val categoryId: Long, val name: String, val spentMinor: Long, val currencyCode: String, val target: BudgetTarget?)

        val basisRows = categoryIds.mapNotNull { categoryId ->
            val name = categories.firstOrNull { it.id == categoryId }?.name ?: return@mapNotNull null
            val target = categoryTargetsById[categoryId]
            val categoryExpenses = expensesByCategory[categoryId].orEmpty()
            // A bar shows one number: the target's currency if a target exists, else this
            // category's largest-total currency this month (ExpenseTotals is still the one
            // place currencies are summed; a bar never adds two codes together).
            val currencyCode = target?.amount?.currencyCode
                ?: ExpenseTotals.byCurrency(categoryExpenses).maxByOrNull { it.minor }?.currencyCode
                ?: currencyCode()
            val spentMinor = categoryExpenses.filter { it.amount.currencyCode == currencyCode }.sumOf { it.amount.minor }
            Basis(categoryId, name, spentMinor, currencyCode, target)
        }.sortedByDescending { it.spentMinor }

        val scale = basisRows.maxOfOrNull { maxOf(it.spentMinor, it.target?.amount?.minor ?: 0L) }?.coerceAtLeast(1L) ?: 1L

        val bars = basisRows.map { basis ->
            BudgetBarUiModel(
                categoryId = basis.categoryId,
                categoryName = basis.name,
                spentFormatted = MoneyFormatter.format(Money(basis.spentMinor, basis.currencyCode), locale),
                fillFraction = (basis.spentMinor.toFloat() / scale.toFloat()).coerceIn(0f, 1f),
                targetFraction = basis.target?.let { (it.amount.minor.toFloat() / scale.toFloat()).coerceIn(0f, 1f) },
                isOver = basis.target != null && basis.spentMinor > basis.target.amount.minor,
            )
        }

        // The meter is discoverable the same way a bar is: spend-or-target, not
        // target-only. Without this, there would be no tappable affordance to ever
        // create the *first* overall target — bars already earn theirs from spend
        // alone (above), and the total needs the same door in.
        val meter = if (overallTarget != null || bars.isNotEmpty()) {
            buildMeter(overallTarget, monthExpenses, locale)
        } else {
            null
        }

        return BudgetOverviewUiModel(meter = meter, bars = bars)
    }

    private fun buildMeter(target: BudgetTarget?, monthExpenses: List<Expense>, locale: Locale): BudgetMeterUiModel {
        if (target == null) {
            // No overall target yet: same currency-choice rule as an untargeted bar.
            val currencyCode = ExpenseTotals.byCurrency(monthExpenses).maxByOrNull { it.minor }?.currencyCode
                ?: currencyCode()
            val spentMinor = monthExpenses.filter { it.amount.currencyCode == currencyCode }.sumOf { it.amount.minor }
            return BudgetMeterUiModel(
                hasTarget = false,
                fillRatio = 0f,
                spentFormatted = MoneyFormatter.format(Money(spentMinor, currencyCode), locale),
                remainingFormatted = null,
                targetFormatted = null,
                isOver = false,
            )
        }

        val progress = BudgetProgress.of(listOf(target), monthExpenses).single()
        return BudgetMeterUiModel(
            hasTarget = true,
            fillRatio = progress.ratio.toFloat().coerceIn(0f, 1f),
            spentFormatted = MoneyFormatter.format(progress.spent, locale),
            remainingFormatted = MoneyFormatter.format(
                Money(abs(progress.remaining.minor), progress.remaining.currencyCode),
                locale,
            ),
            targetFormatted = MoneyFormatter.format(target.amount, locale),
            isOver = progress.isOver,
        )
    }

    private fun today() = clock.instant().atZone(zoneProvider.get()).toLocalDate()

    private fun locale(): Locale = localeProvider.get()

    /** A locale with no country has no currency; fall back rather than crash the write. */
    private fun currencyCode(): String =
        runCatching { Currency.getInstance(locale()).currencyCode }
            .getOrDefault(FALLBACK_CURRENCY_CODE)

    private companion object {
        const val FALLBACK_CURRENCY_CODE = "USD"
    }
}
