package com.quicklogger.app.presentation.log

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    fun onEvent(event: LogEvent) {
        when (event) {
            is LogEvent.AmountChanged -> {
                _uiState.update { it.copy(amountInput = event.value) }
            }
        }
    }
}
