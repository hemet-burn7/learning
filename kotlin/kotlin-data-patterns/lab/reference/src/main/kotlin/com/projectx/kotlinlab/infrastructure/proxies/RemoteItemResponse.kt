package com.projectx.kotlinlab.infrastructure.proxies

import com.fasterxml.jackson.annotation.JsonProperty
import com.projectx.kotlinlab.application.ports.RemoteItemSnapshot

data class RemoteItemResponse(
    @param:JsonProperty("item_name")
    val name: String?,
) {
    // HTTP 응답을 읽은 것과 저장할 값을 정한 것은 서로 다른 단계다.
    fun toSnapshot(): RemoteItemSnapshot = RemoteItemSnapshot(name = name)
}
