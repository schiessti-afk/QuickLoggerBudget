package com.quicklogger.app

import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.usecase.BudgetError
import com.quicklogger.app.domain.usecase.ClearBudgetTarget
import com.quicklogger.app.domain.usecase.ObserveBudgetTargets
import com.quicklogger.app.domain.usecase.SetBudgetTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetTargetUseCasesTest {
    // --- SetBudgetTarget ---

    @Test
    fun setsTheOverallTargetWhenCategoryIdIsNull() = runTest {
        val repo = FakeBudgetTargetRepository()

        SetBudgetTarget(repo)(categoryId = null, amount = Money(10_000, "USD"))

        assertEquals(listOf(BudgetTarget(null, Money(10_000, "USD"))), repo.observeAll().first())
    }

    @Test
    fun setsATargetForASpecificCategory() = runTest {
        val repo = FakeBudgetTargetRepository()

        SetBudgetTarget(repo)(categoryId = 1L, amount = Money(5_000, "USD"))

        assertEquals(listOf(BudgetTarget(1L, Money(5_000, "USD"))), repo.observeAll().first())
    }

    @Test
    fun settingATargetAgainReplacesTheAmountRatherThanAddingASecondRow() = runTest {
        val repo = FakeBudgetTargetRepository(listOf(BudgetTarget(1L, Money(5_000, "USD"))))

        SetBudgetTarget(repo)(categoryId = 1L, amount = Money(9_000, "USD"))

        val targets = repo.observeAll().first()
        assertEquals(1, targets.size)
        assertEquals(9_000L, targets.single().amount.minor)
    }

    @Test
    fun rejectsAZeroAmount() = runTest {
        val result = SetBudgetTarget(FakeBudgetTargetRepository())(categoryId = 1L, amount = Money(0, "USD"))

        assertEquals(BudgetError.InvalidAmount, result.exceptionOrNull())
    }

    @Test
    fun rejectsANegativeAmount() = runTest {
        val result = SetBudgetTarget(FakeBudgetTargetRepository())(categoryId = 1L, amount = Money(-100, "USD"))

        assertEquals(BudgetError.InvalidAmount, result.exceptionOrNull())
    }

    @Test
    fun aRejectedAmountWritesNothing() = runTest {
        val repo = FakeBudgetTargetRepository()

        SetBudgetTarget(repo)(categoryId = 1L, amount = Money(0, "USD"))

        assertTrue(repo.observeAll().first().isEmpty())
    }

    // --- ClearBudgetTarget ---

    @Test
    fun clearsTheOverallTarget() = runTest {
        val repo = FakeBudgetTargetRepository(listOf(BudgetTarget(null, Money(10_000, "USD"))))

        ClearBudgetTarget(repo)(categoryId = null)

        assertTrue(repo.observeAll().first().isEmpty())
    }

    @Test
    fun clearingOneCategorysTargetLeavesOthersAlone() = runTest {
        val repo = FakeBudgetTargetRepository(
            listOf(BudgetTarget(1L, Money(5_000, "USD")), BudgetTarget(2L, Money(3_000, "USD"))),
        )

        ClearBudgetTarget(repo)(categoryId = 1L)

        assertEquals(listOf(BudgetTarget(2L, Money(3_000, "USD"))), repo.observeAll().first())
    }

    @Test
    fun clearingAMissingTargetIsANoOp() = runTest {
        val repo = FakeBudgetTargetRepository()

        ClearBudgetTarget(repo)(categoryId = 1L)

        assertTrue(repo.observeAll().first().isEmpty())
    }

    // --- ObserveBudgetTargets ---

    @Test
    fun observesEveryStandingTarget() = runTest {
        val repo = FakeBudgetTargetRepository(
            listOf(BudgetTarget(null, Money(50_000, "USD")), BudgetTarget(1L, Money(10_000, "USD"))),
        )

        val targets = ObserveBudgetTargets(repo)().first()

        assertEquals(2, targets.size)
    }
}
