package com.quicklogger.app.presentation.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicklogger.app.R
import com.quicklogger.app.presentation.components.PeriodChips
import com.quicklogger.app.presentation.components.buildCsvShareIntent
import com.quicklogger.app.presentation.components.buildTextShareIntent
import com.quicklogger.app.presentation.components.launchShareChooser
import com.quicklogger.app.presentation.theme.categoryStyleFor

@Composable
fun HistoryScreen(
    onNavigateUp: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onManageCategories: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HistoryUiEvent.ShareText -> context.launchShareChooser(buildTextShareIntent(event.text))
                is HistoryUiEvent.ShareCsv ->
                    context.launchShareChooser(buildCsvShareIntent(context, event.fileName))
            }
        }
    }

    HistoryScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
        onEditExpense = onEditExpense,
        onManageCategories = onManageCategories,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreenContent(
    uiState: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    onNavigateUp: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onManageCategories: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.history_menu),
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manage_categories)) },
                            onClick = { showMenu = false; onManageCategories() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_period)) },
                            onClick = { showMenu = false; onEvent(HistoryEvent.SharePeriodText) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_csv)) },
                            onClick = { showMenu = false; onEvent(HistoryEvent.ExportCsv) },
                        )
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PeriodChips(
                selected = uiState.period,
                onSelected = { onEvent(HistoryEvent.PeriodSelected(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            if (uiState.totalsFormatted.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    uiState.totalsFormatted.forEach { total ->
                        Text(total, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.isEmpty) {
                // DESIGN §4.2/§8.3: one sentence plus the illustration, not a
                // marketing checklist. The illustration already carries its own
                // cream/ink palette, so it needs no tint.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_empty_history),
                            contentDescription = null,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(uiState.rows, key = { it.id }) { row ->
                        HistoryRow(row = row, onClick = { onEditExpense(row.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(row: HistoryRowUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(row.amountFormatted, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(categoryStyleFor(row.categoryName).pictogram),
                    contentDescription = null, // decorative; the category name follows in text
                    modifier = Modifier.size(16.dp),
                    tint = categoryStyleFor(row.categoryName).accent,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${row.categoryName} · ${row.occurredAtFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (row.hasReceipt) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.receipt_attached),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
