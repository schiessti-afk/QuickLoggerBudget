package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.CreateCategory
import com.quicklogger.app.domain.usecase.DeleteCategory
import com.quicklogger.app.domain.usecase.RenameCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryUseCasesTest {
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val other = Category(id = 6L, name = "Other", sortOrder = 5, isProtected = true)

    // --- create ---

    @Test
    fun createsATrimmedCategory() = runTest {
        val repo = FakeCategoryRepository(listOf(food, other))

        val result = CreateCategory(repo)("  Groceries  ")

        assertEquals("Groceries", result.getOrThrow().name)
    }

    @Test
    fun rejectsABlankName() = runTest {
        val result = CreateCategory(FakeCategoryRepository())("   ")

        assertEquals(CategoryError.NameRequired, result.exceptionOrNull())
    }

    @Test
    fun rejectsANameOver40Characters() = runTest {
        val result = CreateCategory(FakeCategoryRepository())("x".repeat(41))

        assertEquals(CategoryError.NameTooLong, result.exceptionOrNull())
    }

    @Test
    fun acceptsANameOfExactly40Characters() = runTest {
        val result = CreateCategory(FakeCategoryRepository())("x".repeat(40))

        assertTrue(result.isSuccess)
    }

    @Test
    fun rejectsACaseInsensitiveDuplicate() = runTest {
        val repo = FakeCategoryRepository(listOf(food))

        val result = CreateCategory(repo)("FOOD")

        assertEquals(CategoryError.DuplicateName, result.exceptionOrNull())
    }

    @Test
    fun newCategoriesAppendAfterExistingOnes() = runTest {
        val repo = FakeCategoryRepository(listOf(food, other))

        val created = CreateCategory(repo)("Groceries").getOrThrow()

        assertTrue(created.sortOrder > other.sortOrder)
    }

    // --- rename ---

    @Test
    fun renamesAnExistingCategory() = runTest {
        val repo = FakeCategoryRepository(listOf(food))

        val result = RenameCategory(repo)(food.id, "Groceries")

        assertTrue(result.isSuccess)
        assertEquals("Groceries", repo.observeAll().first().single().name)
    }

    @Test
    fun renamingAMissingCategoryFails() = runTest {
        val result = RenameCategory(FakeCategoryRepository())(999L, "Groceries")

        assertEquals(CategoryError.NotFound, result.exceptionOrNull())
    }

    @Test
    fun renamingToAnotherCategorysNameFails() = runTest {
        val repo = FakeCategoryRepository(listOf(food, other))

        val result = RenameCategory(repo)(food.id, "other")

        assertEquals(CategoryError.DuplicateName, result.exceptionOrNull())
    }

    @Test
    fun renamingToItsOwnNameSucceeds() = runTest {
        val repo = FakeCategoryRepository(listOf(food))

        val result = RenameCategory(repo)(food.id, "Food")

        assertTrue(result.isSuccess)
    }

    // --- delete ---

    @Test
    fun deletingReassignsToTheProtectedCategory() = runTest {
        val repo = FakeCategoryRepository(listOf(food, other))

        val result = DeleteCategory(repo)(food.id)

        assertTrue(result.isSuccess)
        assertEquals(listOf(food.id to other.id), repo.deletions)
    }

    @Test
    fun theProtectedCategoryCannotBeDeleted() = runTest {
        val repo = FakeCategoryRepository(listOf(food, other))

        val result = DeleteCategory(repo)(other.id)

        assertEquals(CategoryError.ProtectedCategory, result.exceptionOrNull())
        assertTrue(repo.deletions.isEmpty())
    }

    @Test
    fun deletingAMissingCategoryFails() = runTest {
        val result = DeleteCategory(FakeCategoryRepository(listOf(other)))(999L)

        assertEquals(CategoryError.NotFound, result.exceptionOrNull())
    }
}
