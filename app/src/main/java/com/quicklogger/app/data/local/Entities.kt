package com.quicklogger.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** ARCHITECTURE §7.1. `name` is unique case-insensitively, hence the NOCASE collation. */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val sortOrder: Int,
    val isProtected: Boolean,
)

/**
 * ARCHITECTURE §7.1. Money is two columns — integer minor units plus the ISO 4217
 * code captured at save time — so nothing is ever stored as a floating-point value.
 *
 * `RESTRICT` is deliberate: sprint 4 deletes a category by first reassigning its
 * expenses to `Other`. A cascade would silently destroy expense rows instead.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["occurredAtEpochMs"], orders = [Index.Order.DESC]),
        Index(value = ["categoryId"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: Long,
    val occurredAtEpochMs: Long,
    val receiptRelativePath: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
