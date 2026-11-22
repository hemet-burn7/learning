package com.projectx.kotlinlab.presentation

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.ALWAYS)
data class RemoteItemView(
    @get:JsonProperty("item_name")
    val name: String?,
)
