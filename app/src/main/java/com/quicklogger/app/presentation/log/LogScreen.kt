package com.quicklogger.app.presentation.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicklogger.app.R
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.SaveExpenseError
import com.quicklogger.app.presentation.components.AmountField
import com.quicklogger.app.presentation.components.CategoryChips
import com.quicklogger.app.presentation.components.ReceiptAttachment
import com.quicklogger.app.presentation.components.receiptFile
import com.quicklogger.app.presentation.components.receiptUri
import java.io.File

@Composable
fun LogScreen(
    onOpenHistory: () -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ARCHITECTURE §3.1: the launchers live here, not in the ViewModel.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> viewModel.onEvent(LogEvent.ReceiptCaptured(success)) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        // A null Uri means the user backed out; nothing was created, nothing to clean up.
        uri?.let { viewModel.onEvent(LogEvent.ReceiptPicked(it.toString())) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is LogUiEvent.LaunchCamera ->
                    cameraLauncher.launch(receiptUri(context, event.relativePath))
            }
        }
    }

    LogScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenHistory = onOpenHistory,
        receiptFile = uiState.receiptRelativePath?.let { receiptFile(context, it) },
        onPickReceipt = {
            pickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogScreenContent(
    uiState: LogUiState,
    onEvent: (LogEvent) -> Unit,
    onOpenHistory: () -> Unit,
    receiptFile: File? = null,
    onPickReceipt: () -> Unit = {},
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

            Spacer(Modifier.height(24.dp))

            ReceiptAttachment(
                receiptFile = receiptFile,
                isAttaching = uiState.isAttachingReceipt,
                onCapture = { onEvent(LogEvent.CaptureReceipt) },
                onPick = onPickReceipt,
                onRemove = { onEvent(LogEvent.RemoveReceipt) },
                errorText = uiState.receiptError?.let { stringResource(it.messageRes) },
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

private val ReceiptError.messageRes: Int
    get() = when (this) {
        ReceiptError.TooLarge -> R.string.error_receipt_too_large
        ReceiptError.Unreadable -> R.string.error_receipt_unreadable
    }
