package com.quicklogger.app.data.repository

import androidx.room.withTransaction
import com.quicklogger.app.data.local.BudgetTargetDao
import com.quicklogger.app.data.local.BudgetTargetEntity
import com.quicklogger.app.data.local.QuickLoggerDatabase
import com.quicklogger.app.data.local.toDomain
import com.quicklogger.app.domain.model.BudgetTarget
import com.quicklogger.app.domain.model.Money
import com.quicklogger.app.domain.repository.BudgetTargetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * "Upsert" (get, then insert or update inside one transaction) lives here rather
 * than in [BudgetTargetDao] — mirrors [RoomCategoryRepository], which does the same
 * for a multi-step write instead of teaching the DAO a composite operation.
 */
class RoomBudgetTargetRepository @Inject constructor(
    private val database: QuickLoggerDatabase,
    private val budgetTargets: BudgetTargetDao,
) : BudgetTargetRepository {
    override fun observeAll(): Flow<List<BudgetTarget>> =
        budgetTargets.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertOverall(amount: Money) {
        database.withTransaction {
            val existing = budgetTargets.getOverall()
            if (existing != null) {
                budgetTargets.update(existing.copy(amountMinor = amount.minor, currencyCode = amount.currencyCode))
            } else {
                budgetTargets.insert(
                    BudgetTargetEntity(categoryId = null, amountMinor = amount.minor, currencyCode = amount.currencyCode),
                )
            }
        }
    }

    override suspend fun upsertForCategory(categoryId: Long, amount: Money) {
        database.withTransaction {
            val existing = budgetTargets.getForCategory(categoryId)
            if (existing != null) {
                budgetTargets.update(existing.copy(amountMinor = amount.minor, currencyCode = amount.currencyCode))
            } else {
                budgetTargets.insert(
                    BudgetTargetEntity(
                        categoryId = categoryId,
                        amountMinor = amount.minor,
                        currencyCode = amount.currencyCode,
                    ),
                )
            }
        }
    }

    override suspend fun deleteOverall() = budgetTargets.deleteOverall()

    override suspend fun deleteForCategory(categoryId: Long) = budgetTargets.deleteForCategory(categoryId)
}
