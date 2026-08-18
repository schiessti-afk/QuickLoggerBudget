package com.quicklogger.app.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.quicklogger.app.R

/**
 * Per-category accent + pictogram (DESIGN §5.2, §8.5). [Category] carries no
 * color/icon column of its own — ARCHITECTURE's schema has none, and adding one is
 * out of this sprint's scope — so seeded categories are matched by their fixed
 * seed name (`QuickLoggerDatabase`'s seed list). A custom (user-created) category,
 * or any name that doesn't match a seeded one, falls back to [OTHER]'s accent and
 * mark (DESIGN §4.4): no generated asset is ever required to resolve a style.
 */
data class CategoryStyle(val accent: Color, @DrawableRes val pictogram: Int)

private val FOOD = CategoryStyle(Color(0xFFC45C3E), R.drawable.ic_category_food)
private val TRANSPORT = CategoryStyle(Color(0xFF3D5A80), R.drawable.ic_category_transport)
private val SUPPLIES = CategoryStyle(Color(0xFF5C7A4A), R.drawable.ic_category_supplies)
private val UTILITIES = CategoryStyle(Color(0xFFC4922A), R.drawable.ic_category_utilities)
private val PERSONAL = CategoryStyle(Color(0xFF8B4D63), R.drawable.ic_category_personal)
private val OTHER = CategoryStyle(Color(0xFF6F675E), R.drawable.ic_category_other)

fun categoryStyleFor(name: String): CategoryStyle = when (name) {
    "Food" -> FOOD
    "Transport" -> TRANSPORT
    "Supplies" -> SUPPLIES
    "Utilities" -> UTILITIES
    "Personal" -> PERSONAL
    else -> OTHER
}
