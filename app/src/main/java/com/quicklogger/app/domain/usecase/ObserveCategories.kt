package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The chip row, in `sortOrder`. */
class ObserveCategories @Inject constructor(
    private val categories: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> = categories.observeAll()
}
