package com.projectx.kotlinlab.infrastructure.eventbus

import com.projectx.kotlinlab.application.events.ItemChangedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class InMemoryReceivedItemStoreTest {
    private val itemId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `동일 버전과 과거 버전은 최신 수신 상태를 덮지 않는다`() {
        val store = InMemoryReceivedItemStore()
        assertTrue(store.accept(event(12, "Beta")))
        assertFalse(store.accept(event(11, "Alpha")))
        assertFalse(store.accept(event(12, "같은 버전의 다른 값")))
        assertEquals("Beta", store.snapshot().single().name)
        assertTrue(store.accept(event(13, "Gamma")))
        assertFalse(store.accept(event(12, "Beta")))
        assertEquals("Gamma", store.snapshot().single().name)
        assertEquals(13L, store.snapshot().single().sourceVersion)
    }

    @Test
    fun `다른 테넌트와 병원의 이벤트는 수신 상태에 반영하지 않는다`() {
        val store = InMemoryReceivedItemStore()
        assertFalse(store.accept(event(1, "Alpha").copy(tenantId = "T2")))
        assertFalse(store.accept(event(1, "Alpha").copy(hospitalCode = "H2")))
        assertTrue(store.snapshot().isEmpty())
    }

    @Test
    fun `항목이 다르면 버전을 독립적으로 비교한다`() {
        val store = InMemoryReceivedItemStore()
        val secondId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        store.accept(event(12, "Beta"))
        assertTrue(store.accept(event(1, "Alpha").copy(itemId = secondId)))
        assertEquals(listOf(12L, 1L), store.snapshot().map { it.sourceVersion })
    }

    @Test
    fun `같은 항목을 동시에 받더라도 가장 높은 버전이 남는다`() {
        val store = InMemoryReceivedItemStore()
        // Kafka나 DB를 띄우지 않고 메모리 저장소의 비교와 교체 경쟁을 확인한다.
        Executors.newFixedThreadPool(4).use { executor ->
            val tasks = (1L..100L).map { version ->
                Callable { store.accept(event(version, "이름 $version")) }
            }
            executor.invokeAll(tasks).forEach { it.get() }
        }
        assertEquals(100L, store.snapshot().single().sourceVersion)
        assertEquals("이름 100", store.snapshot().single().name)
    }

    private fun event(version: Long, name: String) = ItemChangedEvent(itemId, "T1", "H1", name, version)
}
