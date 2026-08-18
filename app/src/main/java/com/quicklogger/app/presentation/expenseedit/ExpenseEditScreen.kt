package com.quicklogger.app.presentation.expenseedit

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicklogger.app.R
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.presentation.components.AmountField
import com.quicklogger.app.presentation.components.CategoryChips
import com.quicklogger.app.presentation.components.ReceiptAttachment
import com.quicklogger.app.presentation.components.receiptFile
import com.quicklogger.app.presentation.components.receiptUri
import com.quicklogger.app.presentation.receipt.ReceiptAttachmentUiEvent
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun ExpenseEditScreen(
    onNavigateUp: () -> Unit,
    viewModel: ExpenseEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> viewModel.onEvent(ExpenseEditEvent.ReceiptCaptured(success)) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.onEvent(ExpenseEditEvent.ReceiptPicked(it.toString())) } }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is ExpenseEditUiEvent.LaunchCamera ->
                    cameraLauncher.launch(receiptUri(context, event.relativePath))
                ExpenseEditUiEvent.NavigateBack -> onNavigateUp()
            }
        }
    }

    ExpenseEditScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
        receiptFile = uiState.receiptRelativePath?.let { receiptFile(context, it) },
        onPickReceipt = {
            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseEditScreenContent(
    uiState: ExpenseEditUiState,
    onEvent: (ExpenseEditEvent) -> Unit,
    onNavigateUp: () -> Unit,
    receiptFile: java.io.File? = null,
    onPickReceipt: () -> Unit = {},
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.notFound) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        when {
            uiState.notFound -> Text(
                text = stringResource(R.string.expense_not_found),
                modifier = Modifier.padding(innerPadding).padding(16.dp),
            )

            uiState.isLoading -> Unit

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                AmountField(
                    formattedAmount = uiState.amountFormatted,
                    onRawInputChange = { onEvent(ExpenseEditEvent.AmountChanged(it)) },
                    label = stringResource(R.string.amount_label),
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
                    onCategorySelected = { onEvent(ExpenseEditEvent.CategorySelected(it)) },
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.occurred_at_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(uiState.occurredAtFormatted)
                }

                Spacer(Modifier.height(24.dp))

                ReceiptAttachment(
                    receiptFile = receiptFile,
                    isAttaching = uiState.isAttachingReceipt,
                    onCapture = { onEvent(ExpenseEditEvent.CaptureReceipt) },
                    onPick = onPickReceipt,
                    onRemove = { onEvent(ExpenseEditEvent.RemoveReceipt) },
                    errorText = uiState.receiptError?.let { stringResource(it.messageRes) },
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { onEvent(ExpenseEditEvent.Save) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSave,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.occurredAt.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDateMillis = dateState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) { Text(stringResource(R.string.next)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val original = uiState.occurredAt.atZone(ZoneOffset.UTC)
        val timeState = rememberTimePickerState(
            initialHour = original.hour,
            initialMinute = original.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dateMillis = pendingDateMillis ?: uiState.occurredAt.toEpochMilli()
                        val combined = Instant.ofEpochMilli(dateMillis)
                            .atZone(ZoneOffset.UTC)
                            .withHour(timeState.hour)
                            .withMinute(timeState.minute)
                            .toInstant()
                        onEvent(ExpenseEditEvent.OccurredAtChanged(combined))
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_expense_title)) },
            text = { Text(stringResource(R.string.delete_expense_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onEvent(ExpenseEditEvent.Delete)
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private val ExpenseError.messageRes: Int
    get() = when (this) {
        ExpenseError.InvalidAmount -> R.string.error_amount_must_be_positive
        ExpenseError.UnknownCategory -> R.string.error_category_missing
    }

private val ReceiptError.messageRes: Int
    get() = when (this) {
        ReceiptError.TooLarge -> R.string.error_receipt_too_large
        ReceiptError.Unreadable -> R.string.error_receipt_unreadable
    }
