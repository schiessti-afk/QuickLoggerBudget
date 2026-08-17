package com.quicklogger.app.domain.model

/**
 * A chip on the Log screen. [isProtected] marks `Other`, the row that cannot be
 * deleted and that orphaned expenses fall back to (ARCHITECTURE §6.3). There is no
 * `isDefault` column: factory rows are simply the ones seeded when the table is empty.
 */
data class Category(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val isProtected: Boolean,
)
