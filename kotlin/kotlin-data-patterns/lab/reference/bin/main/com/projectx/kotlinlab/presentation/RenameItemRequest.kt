package com.projectx.kotlinlab.presentation

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class RenameItemRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    @param:JsonProperty(required = true)
    @field:PositiveOrZero
    val expectedVersion: Long,
    val failAfterFlush: Boolean = false,
)
