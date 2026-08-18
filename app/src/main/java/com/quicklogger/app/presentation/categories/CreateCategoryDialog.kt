package com.quicklogger.app.presentation.categories

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quicklogger.app.R

/**
 * ARCHITECTURE §6.3: category creation is a dialog on Log, not a nav route. Dialog
 * visibility is the caller's local Compose state; this composable only owns the text
 * field draft. Validation (blank, length, duplicate) is domain work — [onCreate]
 * fires unconditionally on a non-blank draft, and the caller surfaces any resulting
 * [com.quicklogger.app.domain.usecase.CategoryError] itself.
 */
@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_create_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(name)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
