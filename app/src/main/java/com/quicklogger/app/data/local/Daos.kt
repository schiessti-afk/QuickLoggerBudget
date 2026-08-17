package com.quicklogger.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Sprint 2 needs reads for the chip row and a newest-first stream. Update/delete
 * land with History corrections in sprint 4. All queries are parameterized.
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY occurredAtEpochMs DESC, id DESC")
    fun observeAllNewestFirst(): Flow<List<ExpenseEntity>>

    /** Returns the auto-generated row id. */
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long
}
