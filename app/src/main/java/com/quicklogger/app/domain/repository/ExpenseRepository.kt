package com.quicklogger.app.domain.repository

import com.quicklogger.app.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Expense persistence as the domain sees it. Implemented in `data/repository`; the
 * domain never sees a Room `@Entity`.
 *
 * Read/update/delete arrive with History and corrections in sprint 4 — this surface
 * carries only what the two-second log path needs.
 */
interface ExpenseRepository {
    fun observeAllNewestFirst(): Flow<List<Expense>>

    /** Returns the stored row, including the id Room assigned. */
    suspend fun insert(expense: Expense): Expense
}
