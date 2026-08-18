package com.quicklogger.app.presentation.dashboard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.quicklogger.app.R
import com.quicklogger.app.presentation.components.AmountField
import androidx.compose.ui.res.stringResource

/**
 * DESIGN §4.2: tapping the meter or a bar opens this — one amount field, reusing
 * Log's digit-buffer [AmountField], plus confirm/cancel. No second screen, no
 * slider. An empty or zero amount clears the target instead of setting it
 * (ARCHITECTURE §6.5), so the confirm label switches to reflect that.
 */
@Composable
fun BudgetTargetDialog(
    state: BudgetTargetDialogUiState,
    onAmountChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isClearing = state.amountDigits.toLongOrNull().let { it == null || it <= 0L }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.categoryName ?: stringResource(R.string.budget_target_dialog_title_overall)) },
        text = {
            AmountField(
                formattedAmount = state.amountFormatted,
                onRawInputChange = onAmountChanged,
                label = stringResource(R.string.budget_target_amount_label),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(
                        if (isClearing) R.string.budget_target_clear else R.string.budget_target_save,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
