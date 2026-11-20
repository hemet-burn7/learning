package com.projectx.kotlinlab.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class RefreshItemNameRequestTest {
    // application.yml의 primitive null 처리와 같은 역직렬화 설정이다.
    private val mapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .build()

    @Test
    fun `필수 버전을 생략하면 0으로 대신 바인딩하지 않는다`() {
        val json = """{"externalId":"alice"}"""
        assertThrows(Exception::class.java) { mapper.readValue(json, RefreshItemNameRequest::class.java) }
    }

    @Test
    fun `필수 버전에 null을 보내면 0으로 대신 바인딩하지 않는다`() {
        val json = """{"externalId":"alice","expectedVersion":null}"""
        assertThrows(Exception::class.java) { mapper.readValue(json, RefreshItemNameRequest::class.java) }
    }

    @Test
    fun `실제로 전달된 버전 0은 정상 입력이다`() {
        val json = """{"externalId":"alice","expectedVersion":0}"""
        val request = mapper.readValue(json, RefreshItemNameRequest::class.java)
        assertEquals("alice", request.externalId)
        assertEquals(0L, request.expectedVersion)
    }
}
