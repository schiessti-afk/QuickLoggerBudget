package com.quicklogger.app.presentation.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicklogger.app.R
import com.quicklogger.app.domain.usecase.SaveExpenseError
import com.quicklogger.app.presentation.components.AmountField
import com.quicklogger.app.presentation.components.CategoryChips

@Composable
fun LogScreen(
    onOpenHistory: () -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LogScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenHistory = onOpenHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogScreenContent(
    uiState: LogUiState,
    onEvent: (LogEvent) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.history),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            AmountField(
                formattedAmount = uiState.amountFormatted,
                onRawInputChange = { onEvent(LogEvent.AmountChanged(it)) },
                label = stringResource(R.string.amount_label),
                modifier = Modifier.focusRequester(focusRequester),
                supportingText = uiState.saveError?.let { stringResource(it.messageRes) },
                isError = uiState.saveError != null,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            CategoryChips(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = { onEvent(LogEvent.CategorySelected(it)) },
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onEvent(LogEvent.Save) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSave,
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/**
 * Domain errors carry no user-facing copy; strings stay in `values/strings.xml`.
 * Save is disabled while the form is invalid, so these are defensive.
 */
private val SaveExpenseError.messageRes: Int
    get() = when (this) {
        SaveExpenseError.InvalidAmount -> R.string.error_amount_must_be_positive
        SaveExpenseError.UnknownCategory -> R.string.error_category_missing
    }
