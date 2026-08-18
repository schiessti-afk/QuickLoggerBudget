package com.quicklogger.app.presentation.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicklogger.app.R
import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.presentation.log.messageRes

/**
 * Rename/delete from History's overflow (ARCHITECTURE §6.3) — a dialog, matching the
 * create dialog's "not a nav route" shape. `Other` never shows a delete action.
 */
@Composable
fun ManageCategoriesDialog(
    onDismiss: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_management_title)) },
        text = {
            Column {
                uiState.error?.let { error ->
                    Text(
                        text = stringResource(error.messageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onRename = { name -> viewModel.onEvent(CategoriesEvent.Rename(category.id, name)) },
                            onDelete = { viewModel.onEvent(CategoriesEvent.Delete(category.id)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun CategoryRow(
    category: Category,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(category.id) { mutableStateOf(category.name) }
    var isEditing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            TextButton(onClick = { onRename(draft); isEditing = false }) {
                Text(stringResource(R.string.rename))
            }
        } else {
            Text(category.name, modifier = Modifier.weight(1f))
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Filled.Create, contentDescription = stringResource(R.string.rename))
            }
            if (!category.isProtected) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}
