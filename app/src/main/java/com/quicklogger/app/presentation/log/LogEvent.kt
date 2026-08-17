package com.quicklogger.app.presentation.log

/**
 * Receipt capture arrives in sprint 3 and Save & Share in sprint 5; neither is
 * stubbed here.
 */
sealed interface LogEvent {
    data class AmountChanged(val raw: String) : LogEvent

    data class CategorySelected(val id: Long) : LogEvent

    data object Save : LogEvent
}
