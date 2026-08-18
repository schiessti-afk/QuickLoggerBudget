package com.quicklogger.app.domain.repository

import com.quicklogger.app.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Expense persistence as the domain sees it. Implemented in `data/repository`; the
 * domain never sees a Room `@Entity`.
 */
interface ExpenseRepository {
    fun observeAllNewestFirst(): Flow<List<Expense>>

    /** [to] is exclusive — see [com.quicklogger.app.domain.model.DateRange]. */
    fun observeInRange(from: Instant, to: Instant): Flow<List<Expense>>

    /** Returns the stored row, including the id Room assigned. */
    suspend fun insert(expense: Expense): Expense

    suspend fun getById(id: Long): Expense?

    suspend fun update(expense: Expense)

    suspend fun delete(id: Long)
}
