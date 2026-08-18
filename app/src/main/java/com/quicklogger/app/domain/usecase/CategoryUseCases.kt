package com.quicklogger.app.domain.usecase

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private fun validateName(raw: String): Result<String> {
    val trimmed = raw.trim()
    return when {
        trimmed.isEmpty() -> Result.failure(CategoryError.NameRequired)
        trimmed.length > CATEGORY_NAME_MAX_LENGTH -> Result.failure(CategoryError.NameTooLong)
        else -> Result.success(trimmed)
    }
}

/** From the Log screen's `+` chip (ARCHITECTURE §6.3): a dialog, not a nav route. */
class CreateCategory @Inject constructor(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(name: String): Result<Category> =
        validateName(name).fold(
            onSuccess = { trimmed -> runCatching { categories.insert(trimmed) } },
            onFailure = { Result.failure(it) },
        )
}

class RenameCategory @Inject constructor(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(id: Long, name: String): Result<Unit> =
        validateName(name).fold(
            onSuccess = { trimmed ->
                if (categories.getById(id) == null) {
                    Result.failure(CategoryError.NotFound)
                } else {
                    runCatching { categories.rename(id, trimmed) }
                }
            },
            onFailure = { Result.failure(it) },
        )
}

/**
 * Deletes a category after reassigning its expenses to the protected row
 * (ARCHITECTURE §6.3). `Other` itself can never be deleted.
 */
class DeleteCategory @Inject constructor(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        val target = categories.getById(id) ?: return Result.failure(CategoryError.NotFound)
        if (target.isProtected) return Result.failure(CategoryError.ProtectedCategory)

        val protected = categories.observeAll().first().firstOrNull { it.isProtected }
            ?: return Result.failure(CategoryError.NotFound)

        return runCatching { categories.delete(id, protected.id) }
    }
}
