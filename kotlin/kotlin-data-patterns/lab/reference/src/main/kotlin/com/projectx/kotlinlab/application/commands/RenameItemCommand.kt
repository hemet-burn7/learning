package com.projectx.kotlinlab.application.commands

import java.util.UUID

data class RenameItemCommand(
    val id: UUID,
    val name: String,
    val expectedVersion: Long,
    val failAfterFlush: Boolean = false,
)
