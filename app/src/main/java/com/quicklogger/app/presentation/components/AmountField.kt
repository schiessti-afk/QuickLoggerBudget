package com.quicklogger.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The amount entry field. The ViewModel owns a digit buffer and hands back a
 * formatted string, so the text is rewritten on every keystroke — the caret is
 * pinned to the end to stop it jumping when the currency symbol or a thousands
 * separator appears.
 */
@Composable
fun AmountField(
    formattedAmount: String,
    onRawInputChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    // Rebuilt on every recomposition rather than remembered: when input is rejected
    // (a 13th digit, a pasted letter) the formatted string is unchanged, and a
    // remembered value would leave the rejected character sitting on screen.
    val fieldValue = TextFieldValue(formattedAmount, TextRange(formattedAmount.length))

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { onRawInputChange(it.text) },
        modifier = modifier.fillMaxWidth(),
        // DESIGN §5.3: tabular/lining figures so the value doesn't jump while
        // typing, on platform faces that support the "tnum" OpenType feature.
        textStyle = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
