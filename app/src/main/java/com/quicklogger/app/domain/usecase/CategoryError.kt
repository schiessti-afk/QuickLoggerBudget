package com.quicklogger.app.domain.usecase

sealed class CategoryError(message: String) : Exception(message) {
    data object NameRequired : CategoryError("Category name cannot be empty")
    data object NameTooLong : CategoryError("Category name is too long")
    data object DuplicateName : CategoryError("A category with this name already exists")
    data object NotFound : CategoryError("Category no longer exists")
    data object ProtectedCategory : CategoryError("This category cannot be deleted")
}

/** ARCHITECTURE §6.3: "length-capped (~40)". */
internal const val CATEGORY_NAME_MAX_LENGTH = 40
