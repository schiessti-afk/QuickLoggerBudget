package com.quicklogger.app.di

import android.content.Context
import androidx.room.Room
import com.quicklogger.app.data.local.CategoryDao
import com.quicklogger.app.data.local.ExpenseDao
import com.quicklogger.app.data.local.QuickLoggerDatabase
import com.quicklogger.app.data.local.SeedCategoriesCallback
import com.quicklogger.app.data.preferences.SharedPreferencesLastCategoryStore
import com.quicklogger.app.data.receipt.ReceiptFileStore
import com.quicklogger.app.data.repository.RoomCategoryRepository
import com.quicklogger.app.data.repository.RoomExpenseRepository
import com.quicklogger.app.domain.repository.CategoryRepository
import com.quicklogger.app.domain.repository.ExpenseRepository
import com.quicklogger.app.domain.repository.LastCategoryStore
import com.quicklogger.app.domain.repository.ReceiptStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuickLoggerDatabase =
        Room.databaseBuilder(context, QuickLoggerDatabase::class.java, QuickLoggerDatabase.NAME)
            .addCallback(SeedCategoriesCallback)
            .build()

    @Provides
    fun provideCategoryDao(database: QuickLoggerDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideExpenseDao(database: QuickLoggerDatabase): ExpenseDao = database.expenseDao()

    /** Injected so use cases can be tested against a fixed instant. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    /**
     * Unscoped on purpose: injected as a `Provider`, every `get()` re-reads the
     * current default so a locale change is picked up without a process restart.
     */
    @Provides
    fun provideLocale(): Locale = Locale.getDefault()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExpenseRepository(impl: RoomExpenseRepository): ExpenseRepository

    @Binds
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindLastCategoryStore(
        impl: SharedPreferencesLastCategoryStore,
    ): LastCategoryStore

    @Binds
    abstract fun bindReceiptStore(impl: ReceiptFileStore): ReceiptStore
}
