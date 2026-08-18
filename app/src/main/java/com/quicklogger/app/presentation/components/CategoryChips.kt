package com.quicklogger.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quicklogger.app.R
import com.quicklogger.app.domain.model.Category

/**
 * Radio-mode category chips (ARCHITECTURE §8.1): exactly one is selected, tapping
 * another moves the selection, and tapping the selected one does nothing.
 *
 * Pictograms and per-category accents are sprint 6; these are plain chips. The
 * trailing `+` chip (ARCHITECTURE §6.3) is not part of the radio group — it opens
 * the create-category dialog and is omitted entirely when [onAddCategory] is null,
 * so History's category list (which doesn't create) can reuse this composable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onAddCategory: (() -> Unit)? = null,
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
        if (onAddCategory != null) {
            FilterChip(
                selected = false,
                onClick = onAddCategory,
                label = { Text(stringResource(R.string.category_add)) },
            )
        }
    }
}
