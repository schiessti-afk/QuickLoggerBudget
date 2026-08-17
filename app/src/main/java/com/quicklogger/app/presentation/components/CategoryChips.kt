package com.quicklogger.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quicklogger.app.domain.model.Category

/**
 * Radio-mode category chips (ARCHITECTURE §8.1): exactly one is selected, tapping
 * another moves the selection, and tapping the selected one does nothing.
 *
 * Pictograms and per-category accents are sprint 6; these are plain chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val isSelected = category.id == selectedCategoryId
            FilterChip(
                selected = isSelected,
                onClick = { if (!isSelected) onCategorySelected(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}
