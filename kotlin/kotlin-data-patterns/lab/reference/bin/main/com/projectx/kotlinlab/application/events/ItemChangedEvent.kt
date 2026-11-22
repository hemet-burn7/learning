package com.projectx.kotlinlab.application.events

import java.util.UUID

data class ItemChangedEvent(
    val itemId: UUID,
    val tenantId: String,
    val hospitalCode: String,
    val name: String,
    val sourceVersion: Long,
)
