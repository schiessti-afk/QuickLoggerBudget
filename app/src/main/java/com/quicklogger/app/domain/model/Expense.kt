package com.quicklogger.app.domain.model

import java.time.Instant

/** A persisted expense. `id == 0L` means it has not been written yet. */
data class Expense(
    val id: Long,
    val amount: Money,
    val categoryId: Long,
    val occurredAt: Instant,
    val receiptRelativePath: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * What the Log screen collects before anything is persisted. `occurredAt` is absent
 * on purpose: it defaults to the save-time clock, and the primary path has no date
 * picker (ARCHITECTURE §6.2).
 */
data class NewExpense(
    val amount: Money,
    val categoryId: Long,
    val receiptRelativePath: String? = null,
)
