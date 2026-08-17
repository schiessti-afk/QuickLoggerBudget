package com.quicklogger.app.domain.repository

/**
 * Remembers which chip was selected last so a cold start needs no tap
 * (ARCHITECTURE §6.3). Backed by SharedPreferences in the data layer.
 *
 * Both calls suspend: the first SharedPreferences access loads the file from disk,
 * and that does not belong on the main thread.
 */
interface LastCategoryStore {
    suspend fun lastSelectedId(): Long?

    suspend fun setLastSelectedId(id: Long)
}
