package com.projectx.kotlinlab.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class RenameItemRequestTest {
    // application.yml의 primitive null 처리와 같은 역직렬화 설정이다.
    private val mapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .build()

    @Test
    fun `필수 버전이 없거나 null이면 0으로 대신 바인딩하지 않는다`() {
        for (json in listOf("""{"name":"Beta"}""", """{"name":"Beta","expectedVersion":null}""")) {
            assertThrows(Exception::class.java) { mapper.readValue(json, RenameItemRequest::class.java) }
        }
    }

    @Test
    fun `실제로 전달된 버전 0은 정상 입력이다`() {
        val request = mapper.readValue("""{"name":"Beta","expectedVersion":0}""", RenameItemRequest::class.java)
        assertEquals(0L, request.expectedVersion)
        assertEquals(false, request.failAfterFlush)
    }
}
