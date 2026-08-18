package com.quicklogger.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.quicklogger.app.data.local.MIGRATION_1_2
import com.quicklogger.app.data.local.QuickLoggerDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * ARCHITECTURE §7.1 / §15: the one approved post-v1 schema change. Proves the
 * migration against the *committed* `1.json`, not only a fresh install — an app
 * updated from v1 must keep every expense, category, and receipt.
 */
class DatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        QuickLoggerDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsExistingCategoriesAndExpenses() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO categories (id, name, sortOrder, isProtected) VALUES (1, 'Food', 0, 0)")
            execSQL(
                "INSERT INTO expenses " +
                    "(id, amountMinor, currencyCode, categoryId, occurredAtEpochMs, receiptRelativePath, createdAtEpochMs, updatedAtEpochMs) " +
                    "VALUES (1, 4500, 'USD', 1, 1000, NULL, 1000, 1000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT * FROM categories").use { cursor ->
            assertEquals(1, cursor.count)
        }
        db.query("SELECT * FROM expenses").use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    @Test
    fun migrate1To2CreatesAnEmptyBudgetTargetsTable() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT * FROM budget_targets").use { cursor ->
            assertEquals(0, cursor.count)
        }
    }

    @Test
    fun migrate1To2AllowsInsertingATargetAfterwards() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO categories (id, name, sortOrder, isProtected) VALUES (1, 'Food', 0, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        db.execSQL("INSERT INTO budget_targets (categoryId, amountMinor, currencyCode) VALUES (1, 5000, 'USD')")

        db.query("SELECT * FROM budget_targets WHERE categoryId = 1").use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
