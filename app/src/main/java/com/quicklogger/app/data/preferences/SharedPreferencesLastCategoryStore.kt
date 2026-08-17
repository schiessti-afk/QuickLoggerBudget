package com.quicklogger.app.data.preferences

import android.content.Context
import com.quicklogger.app.domain.repository.LastCategoryStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ARCHITECTURE §6.3: the last selected chip id, so a cold start needs no tap.
 * SharedPreferences rather than DataStore — one `Long` does not earn a new
 * dependency.
 */
@Singleton
class SharedPreferencesLastCategoryStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LastCategoryStore {
    private val preferences by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun lastSelectedId(): Long? = withContext(Dispatchers.IO) {
        // The lazy load above touches disk on first access, hence the IO hop.
        preferences.getLong(KEY_LAST_CATEGORY_ID, NONE).takeIf { it != NONE }
    }

    override suspend fun setLastSelectedId(id: Long) = withContext(Dispatchers.IO) {
        preferences.edit().putLong(KEY_LAST_CATEGORY_ID, id).apply()
    }

    private companion object {
        const val FILE_NAME = "quicklogger_prefs"
        const val KEY_LAST_CATEGORY_ID = "last_category_id"
        const val NONE = -1L
    }
}
