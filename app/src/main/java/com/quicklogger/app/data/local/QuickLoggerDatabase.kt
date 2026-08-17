package com.quicklogger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class QuickLoggerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME = "quicklogger.db"
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
