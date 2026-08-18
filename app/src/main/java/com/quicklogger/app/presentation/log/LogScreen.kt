package com.quicklogger.app.presentation.log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicklogger.app.R
import com.quicklogger.app.domain.repository.ReceiptError
import com.quicklogger.app.domain.usecase.CategoryError
import com.quicklogger.app.domain.usecase.ExpenseError
import com.quicklogger.app.presentation.categories.CreateCategoryDialog
import com.quicklogger.app.presentation.components.AmountField
import com.quicklogger.app.presentation.components.CategoryChips
import com.quicklogger.app.presentation.components.ReceiptAttachment
import com.quicklogger.app.presentation.components.buildReceiptShareIntent
import com.quicklogger.app.presentation.components.buildTextShareIntent
import com.quicklogger.app.presentation.components.launchShareChooser
import com.quicklogger.app.presentation.components.receiptFile
import com.quicklogger.app.presentation.components.receiptUri
import com.quicklogger.app.presentation.theme.QuickLoggerButtonShape
import java.io.File

@Composable
fun LogScreen(
    onOpenDashboard: () -> Unit,
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
                is LogUiEvent.Share -> context.launchShareChooser(
                    if (event.receiptRelativePath != null) {
                        buildReceiptShareIntent(context, event.text, event.receiptRelativePath)
                    } else {
                        buildTextShareIntent(event.text)
                    },
                )
            }
        }
    }

    LogScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenDashboard = onOpenDashboard,
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
    onOpenDashboard: () -> Unit,
    receiptFile: File? = null,
    onPickReceipt: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // DESIGN §8.2: the same receipt-fold silhouette as the
                        // launcher, simplified to 24 dp. Decorative — the title
                        // text next to it is what's announced.
                        Icon(
                            painter = painterResource(R.drawable.ic_toolbar_receipt),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDashboard) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.dashboard),
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

            // ARCHITECTURE §8.1.8 / DESIGN §4.1: one line, live against the buffer,
            // absent when neither target exists. Never wraps or pushes the chips down.
            val budgetSegments = listOfNotNull(
                uiState.categoryBudgetLine?.let { budgetLineText(it) },
                uiState.monthBudgetLine?.let { budgetLineText(it) },
            )
            if (budgetSegments.isNotEmpty()) {
                val isOver = uiState.categoryBudgetLine?.isOver == true || uiState.monthBudgetLine?.isOver == true
                Text(
                    text = budgetSegments.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

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
                onAddCategory = { showCreateCategoryDialog = true },
            )

            uiState.categoryError?.let { error ->
                Text(
                    text = stringResource(error.messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )

            Spacer(Modifier.height(16.dp))

            ReceiptAttachment(
                receiptFile = receiptFile,
                isAttaching = uiState.isAttachingReceipt,
                onCapture = { onEvent(LogEvent.CaptureReceipt) },
                onPick = onPickReceipt,
                onRemove = { onEvent(LogEvent.RemoveReceipt) },
                errorText = uiState.receiptError?.let { stringResource(it.messageRes) },
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onEvent(LogEvent.Save) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canSave,
                    shape = QuickLoggerButtonShape,
                ) {
                    Text(stringResource(R.string.save))
                }
                OutlinedButton(
                    onClick = { onEvent(LogEvent.SaveAndShare) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canSave,
                    shape = QuickLoggerButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (uiState.canSave) 1f else 0.38f,
                        ),
                    ),
                ) {
                    Text(stringResource(R.string.save_and_share))
                }
            }
        }
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onCreate = { onEvent(LogEvent.CreateCategoryRequested(it)) },
        )
    }
}

/** Picks the "left" or "over by" template (DESIGN §5.4) and fills it from `strings.xml`. */
@Composable
private fun budgetLineText(line: BudgetLineUiModel): String = if (line.isOver) {
    stringResource(R.string.budget_over_by, line.label, line.remainingFormatted)
} else {
    stringResource(R.string.budget_left, line.label, line.remainingFormatted)
}

/**
 * Domain errors carry no user-facing copy; strings stay in `values/strings.xml`.
 * Save is disabled while the form is invalid, so these are defensive.
 */
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

internal val CategoryError.messageRes: Int
    get() = when (this) {
        CategoryError.NameRequired -> R.string.error_category_name_required
        CategoryError.NameTooLong -> R.string.error_category_name_too_long
        CategoryError.DuplicateName -> R.string.error_category_duplicate_name
        CategoryError.NotFound -> R.string.error_category_not_found
        CategoryError.ProtectedCategory -> R.string.error_category_protected
    }
