package com.quicklogger.app.data.local

import com.quicklogger.app.domain.model.Category
import com.quicklogger.app.domain.model.Expense
import com.quicklogger.app.domain.model.Money
import java.time.Instant

/**
 * Entity ↔ domain translation. The domain layer never sees a Room type, so every
 * crossing goes through here (ARCHITECTURE §3.3).
 */
internal fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    sortOrder = sortOrder,
    isProtected = isProtected,
)

internal fun ExpenseEntity.toDomain() = Expense(
    id = id,
    amount = Money(minor = amountMinor, currencyCode = currencyCode),
    categoryId = categoryId,
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMs),
    receiptRelativePath = receiptRelativePath,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
)

internal fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amountMinor = amount.minor,
    currencyCode = amount.currencyCode,
    categoryId = categoryId,
    occurredAtEpochMs = occurredAt.toEpochMilli(),
    receiptRelativePath = receiptRelativePath,
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
)
