package com.quicklogger.app

import com.quicklogger.app.presentation.log.LogEvent
import com.quicklogger.app.presentation.log.LogViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class LogViewModelTest {
    @Test
    fun amountChangedUpdatesState() {
        val viewModel = LogViewModel()

        viewModel.onEvent(LogEvent.AmountChanged("45"))

        assertEquals("45", viewModel.uiState.value.amountInput)
    }

    @Test
    fun startsWithEmptyAmount() {
        val viewModel = LogViewModel()

        assertEquals("", viewModel.uiState.value.amountInput)
    }
}
