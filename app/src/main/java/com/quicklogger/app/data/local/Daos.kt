package com.quicklogger.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** All queries are parameterized. */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** `isProtected = 0` is a second line of defense against ever deleting `Other`. */
    @Query("DELETE FROM categories WHERE id = :id AND isProtected = 0")
    suspend fun deleteUnprotected(id: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY occurredAtEpochMs DESC, id DESC")
    fun observeAllNewestFirst(): Flow<List<ExpenseEntity>>

    /** `toEpochMs` is exclusive. */
    @Query(
        "SELECT * FROM expenses WHERE occurredAtEpochMs >= :fromEpochMs " +
            "AND occurredAtEpochMs < :toEpochMs ORDER BY occurredAtEpochMs DESC, id DESC",
    )
    fun observeInRange(fromEpochMs: Long, toEpochMs: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    /** Returns the auto-generated row id. */
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    /** One statement, so deleting a category can reassign every row atomically. */
    @Query(
        "UPDATE expenses SET categoryId = :toId, updatedAtEpochMs = :updatedAtEpochMs " +
            "WHERE categoryId = :fromId",
    )
    suspend fun reassignCategory(fromId: Long, toId: Long, updatedAtEpochMs: Long)
}

/**
 * Plain CRUD only — the get-then-insert-or-update "upsert" logic lives in
 * `RoomBudgetTargetRepository.withTransaction`, mirroring how [CategoryDao] stays
 * simple and [RoomCategoryRepository] owns the multi-step work.
 */
@Dao
interface BudgetTargetDao {
    @Query("SELECT * FROM budget_targets ORDER BY categoryId IS NULL DESC, categoryId ASC")
    fun observeAll(): Flow<List<BudgetTargetEntity>>

    @Query("SELECT * FROM budget_targets WHERE categoryId IS NULL")
    suspend fun getOverall(): BudgetTargetEntity?

    @Query("SELECT * FROM budget_targets WHERE categoryId = :categoryId")
    suspend fun getForCategory(categoryId: Long): BudgetTargetEntity?

    @Insert
    suspend fun insert(target: BudgetTargetEntity): Long

    @Update
    suspend fun update(target: BudgetTargetEntity)

    @Query("DELETE FROM budget_targets WHERE categoryId IS NULL")
    suspend fun deleteOverall()

    @Query("DELETE FROM budget_targets WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)
}
