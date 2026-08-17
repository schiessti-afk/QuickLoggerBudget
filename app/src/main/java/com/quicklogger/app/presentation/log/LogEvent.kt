package com.quicklogger.app.presentation.log

sealed interface LogEvent {
    data class AmountChanged(val value: String) : LogEvent
}
