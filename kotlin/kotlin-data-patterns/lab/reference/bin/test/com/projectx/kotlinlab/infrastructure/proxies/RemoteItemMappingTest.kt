package com.projectx.kotlinlab.infrastructure.proxies

import com.projectx.kotlinlab.presentation.RemoteItemView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class RemoteItemMappingTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `외부 JSON 필드가 내부 이름에 바인딩된다`() {
        val response = mapper.readValue("""{"item_name":"Alice"}""", RemoteItemResponse::class.java)
        assertEquals("Alice", response.toSnapshot().name)
    }

    @Test
    fun `null과 빈 문자열을 서로 바꾸지 않는다`() {
        val absent = mapper.readValue("""{"item_name":null}""", RemoteItemResponse::class.java)
        val empty = mapper.readValue("""{"item_name":""}""", RemoteItemResponse::class.java)
        assertNull(absent.toSnapshot().name)
        assertEquals("", empty.toSnapshot().name)
    }

    @Test
    fun `가상 원격 응답도 정해진 외부 필드명과 null을 사용한다`() {
        val json = mapper.readTree(mapper.writeValueAsString(RemoteItemView(name = null)))
        assertTrue(json.has("item_name"))
        assertTrue(json.get("item_name").isNull)
        assertFalse(json.has("name"))
    }
}
