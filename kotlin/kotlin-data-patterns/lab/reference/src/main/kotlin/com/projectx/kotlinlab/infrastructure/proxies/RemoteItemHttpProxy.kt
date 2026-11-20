package com.projectx.kotlinlab.infrastructure.proxies

import com.projectx.kotlinlab.application.ports.RemoteItemPort
import com.projectx.kotlinlab.application.ports.RemoteItemSnapshot
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RemoteItemHttpProxy(
    builder: RestClient.Builder,
    @Value("\${lab.remote.base-url}") baseUrl: String,
) : RemoteItemPort {
    private val client = builder.baseUrl(baseUrl).build()

    override fun find(externalId: String): RemoteItemSnapshot {
        // body에서 HTTP 응답 본문을 읽고 RemoteItemResponse로 역직렬화한다.
        val response = client.get()
            .uri("/lab/remote/items/{externalId}", externalId)
            .retrieve()
            .body(RemoteItemResponse::class.java)
            ?: error("외부 실습 API의 응답 본문이 없습니다.")
        return response.toSnapshot()
    }
}
