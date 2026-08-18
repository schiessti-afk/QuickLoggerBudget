package com.quicklogger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class, BudgetTargetEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class QuickLoggerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun budgetTargetDao(): BudgetTargetDao

    companion object {
        const val NAME = "quicklogger.db"
    }
}

/**
 * ARCHITECTURE §7.1 / §15: the one approved post-v1 schema change, sprint 7. Adds
 * `budget_targets` only — it touches no existing table, so an app updated from v1
 * keeps every expense, category, and receipt untouched.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budget_targets` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`categoryId` INTEGER, " +
                "`amountMinor` INTEGER NOT NULL, " +
                "`currencyCode` TEXT NOT NULL, " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_targets_categoryId` " +
                "ON `budget_targets` (`categoryId`)",
        )
    }
}

/** ARCHITECTURE §6.3 seed order. `Other` is the protected fallback row. */
internal val DEFAULT_CATEGORIES = listOf(
    "Food" to false,
    "Transport" to false,
    "Supplies" to false,
    "Utilities" to false,
    "Personal" to false,
    "Other" to true,
)

/**
 * Seeds the six defaults when the database file is first created.
 *
 * This writes raw SQL against the [SupportSQLiteDatabase] Room hands to the
 * callback rather than going through [CategoryDao]: `onCreate` runs while the
 * database is still being created, and calling back into the DAO would re-enter it.
 */
object SeedCategoriesCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DEFAULT_CATEGORIES.forEachIndexed { index, (name, isProtected) ->
            db.execSQL(
                "INSERT INTO categories (name, sortOrder, isProtected) VALUES (?, ?, ?)",
                arrayOf<Any>(name, index, if (isProtected) 1 else 0),
            )
        }
    }
}
