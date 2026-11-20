package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.infrastructure.eventbus.ReceivedItemState

data class ReceivedEventsView(
    val kafkaEnabled: Boolean,
    val items: List<ReceivedItemState>,
    val storage: String = "MEMORY_ONLY",
    val survivesRestart: Boolean = false,
)
