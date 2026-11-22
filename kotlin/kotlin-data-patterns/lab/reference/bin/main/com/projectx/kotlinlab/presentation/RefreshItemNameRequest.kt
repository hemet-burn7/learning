package com.projectx.kotlinlab.presentation

import com.fasterxml.jackson.annotation.JsonProperty

data class RefreshItemNameRequest(
    val externalId: String,
    @param:JsonProperty(required = true)
    val expectedVersion: Long,
)
