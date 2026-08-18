package com.quicklogger.app.presentation.navigation

object Routes {
    const val LOG = "log"

    /** Renamed from `history` in sprint 7 (ARCHITECTURE §8): same screen, budget overview on top. */
    const val DASHBOARD = "dashboard"
    const val EXPENSE_EDIT = "expense_edit/{id}"

    fun expenseEdit(id: Long) = "expense_edit/$id"
}
