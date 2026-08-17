package com.quicklogger.app.data.repository

import com.quicklogger.app.data.local.ExpenseDao
import com.quicklogger.app.data.local.toDomain
import com.quicklogger.app.data.local.toEntity
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomExpenseRepository @Inject constructor(
    private val expenses: ExpenseDao,
) : ExpenseRepository {
    override fun observeAllNewestFirst(): Flow<List<Expense>> =
        expenses.observeAllNewestFirst().map { rows -> rows.map { it.toDomain() } }

    override suspend fun insert(expense: Expense): Expense =
        expense.copy(id = expenses.insert(expense.toEntity()))
}
