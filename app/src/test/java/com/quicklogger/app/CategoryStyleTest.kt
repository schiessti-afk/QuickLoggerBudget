package com.quicklogger.app

import com.quicklogger.app.R
import com.quicklogger.app.presentation.theme.categoryStyleFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * DESIGN §5.2/§4.4: each seeded category name resolves to its own accent and
 * pictogram; anything else — including a custom (user-created) category — falls
 * back to Other's, so a style always resolves without a generated asset lookup miss.
 */
class CategoryStyleTest {
    @Test
    fun eachSeededNameResolvesToADistinctPictogram() {
        val pictograms = listOf("Food", "Transport", "Supplies", "Utilities", "Personal", "Other")
            .map { categoryStyleFor(it).pictogram }

        assertEquals(pictograms.size, pictograms.toSet().size)
    }

    @Test
    fun eachSeededNameResolvesToADistinctAccent() {
        val accents = listOf("Food", "Transport", "Supplies", "Utilities", "Personal", "Other")
            .map { categoryStyleFor(it).accent }

        assertEquals(accents.size, accents.toSet().size)
    }

    @Test
    fun foodResolvesToItsOwnAccentAndPictogram() {
        val style = categoryStyleFor("Food")

        assertEquals(R.drawable.ic_category_food, style.pictogram)
        assertNotEquals(categoryStyleFor("Other").accent, style.accent)
    }

    @Test
    fun aCustomCategoryNameFallsBackToOther() {
        val custom = categoryStyleFor("Groceries")
        val other = categoryStyleFor("Other")

        assertEquals(other, custom)
    }

    @Test
    fun otherItselfResolvesToTheOtherPictogram() {
        assertEquals(R.drawable.ic_category_other, categoryStyleFor("Other").pictogram)
    }
}
