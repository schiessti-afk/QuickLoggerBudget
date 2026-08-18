package com.quicklogger.app.domain.repository

import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Money
import kotlinx.coroutines.flow.Flow

/**
 * Budget target persistence as the domain sees it (ARCHITECTURE §6.5). Implemented
 * in `data/repository`; the domain never sees a Room `@Entity`.
 *
 * At most one overall target ([upsertOverall]) and at most one target per category
 * ([upsertForCategory]) can exist at a time — "upsert" because setting a target that
 * already exists replaces its amount rather than adding a second row.
 */
interface BudgetTargetRepository {
    fun observeAll(): Flow<List<BudgetTarget>>

    suspend fun upsertOverall(amount: Money)

    suspend fun upsertForCategory(categoryId: Long, amount: Money)

    suspend fun deleteOverall()

    suspend fun deleteForCategory(categoryId: Long)
}
