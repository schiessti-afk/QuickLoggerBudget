package com.quicklogger.app

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.DeleteCategory
import com.quicklogger.app.domain.usecase.ObserveCategories
import com.quicklogger.app.domain.usecase.RenameCategory
import com.quicklogger.app.presentation.categories.CategoriesEvent
import com.quicklogger.app.presentation.categories.CategoriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val food = Category(id = 1L, name = "Food", sortOrder = 0, isProtected = false)
    private val other = Category(id = 6L, name = "Other", sortOrder = 5, isProtected = true)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(categories: List<Category> = listOf(food, other)): Pair<CategoriesViewModel, FakeCategoryRepository> {
        val repo = FakeCategoryRepository(categories)
        return CategoriesViewModel(ObserveCategories(repo), RenameCategory(repo), DeleteCategory(repo)) to repo
    }

    @Test
    fun categoriesReachTheState() = runTest {
        val (viewModel, _) = viewModel()

        advanceUntilIdle()

        assertEquals(listOf(food, other), viewModel.uiState.value.categories)
    }

    @Test
    fun renamingUpdatesTheList() = runTest {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(CategoriesEvent.Rename(food.id, "Groceries"))
        advanceUntilIdle()

        assertEquals("Groceries", viewModel.uiState.value.categories.single { it.id == food.id }.name)
    }

    @Test
    fun renamingToADuplicateSurfacesAnError() = runTest {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(CategoriesEvent.Rename(food.id, "other"))
        advanceUntilIdle()

        assertEquals(CategoryError.DuplicateName, viewModel.uiState.value.error)
    }

    @Test
    fun deletingRemovesTheCategory() = runTest {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(CategoriesEvent.Delete(food.id))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.categories.none { it.id == food.id })
    }

    @Test
    fun deletingTheProtectedCategorySurfacesAnErrorAndKeepsIt() = runTest {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(CategoriesEvent.Delete(other.id))
        advanceUntilIdle()

        assertEquals(CategoryError.ProtectedCategory, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.categories.any { it.id == other.id })
    }

    @Test
    fun dismissingTheErrorClearsIt() = runTest {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(CategoriesEvent.Delete(other.id))
        advanceUntilIdle()

        viewModel.onEvent(CategoriesEvent.DismissError)

        assertNull(viewModel.uiState.value.error)
    }
}
