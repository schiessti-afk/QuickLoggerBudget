package com.quicklogger.app.presentation.navigation

object Routes {
    const val LOG = "log"
    const val HISTORY = "history"
    const val EXPENSE_EDIT = "expense_edit/{id}"

    fun expenseEdit(id: Long) = "expense_edit/$id"
}
