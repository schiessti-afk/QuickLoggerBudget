package com.quicklogger.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quicklogger.app.R
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.presentation.theme.categoryStyleFor

/**
 * Radio-mode category chips (ARCHITECTURE §8.1): exactly one is selected, tapping
 * another moves the selection, and tapping the selected one does nothing.
 *
 * Each chip carries its category's pictogram and accent (DESIGN §5.2, §8.5): an
 * accent-tinted outline and pictogram when unselected, an accent-tinted (~24%)
 * fill when selected. The label always stays ink — DESIGN §7 is explicit that the
 * selected state must never read as white-on-accent. The trailing `+` chip
 * (ARCHITECTURE §6.3) is not part of the radio group — it opens the create-category
 * dialog, carries no pictogram, and is omitted entirely when [onAddCategory] is
 * null, so the Dashboard's category list (which doesn't create) can reuse this composable.
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
            val style = categoryStyleFor(category.name)
            FilterChip(
                selected = isSelected,
                onClick = { if (!isSelected) onCategorySelected(category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(style.pictogram),
                        // Decorative: the chip label already names the category
                        // (DESIGN §7). The launcher icon is the one exception.
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = style.accent,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    iconColor = style.accent,
                    selectedContainerColor = style.accent.copy(alpha = 0.24f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = style.accent,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = style.accent,
                    selectedBorderColor = style.accent,
                ),
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
