package com.projectx.kotlinlab.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ItemTest {
    @Test
    fun `새 항목은 미저장 버전과 입력한 상태를 가진다`() {
        val item = Item.create(LabScope.LOCAL, " Alpha ", false, null)

        assertEquals("Alpha", item.name)
        assertEquals(false, item.active)
        assertEquals("T1", item.tenantId)
        assertEquals("H1", item.hospitalCode)
        assertNull(item.amount)
        assertNull(item.version)
    }

    @Test
    fun `잘못된 이름으로 바꾸면 기존 상태를 유지한다`() {
        val item = Item.create(LabScope.LOCAL, "Alpha", true, BigDecimal("10.50"))

        val exception = assertThrows(LabException::class.java) { item.changeName("  ") }

        assertEquals(LabError.INVALID_INPUT, exception.error)
        assertEquals("Alpha", item.name)
    }

    @Test
    fun `빈 이름과 최대 길이를 넘긴 이름은 생성할 수 없다`() {
        for (name in listOf("  ", "A".repeat(121))) {
            assertThrows(LabException::class.java) { Item.create(LabScope.LOCAL, name, true, null) }
        }
    }

    @Test
    fun `새 이름을 검증하고 공백을 정리한다`() {
        val item = Item.create(LabScope.LOCAL, "Alpha", true, null)
        item.changeName(" Beta ")
        assertEquals("Beta", item.name)
        // 이 테스트에는 EntityManager가 없으므로 DB 저장이나 버전 증가를 검증하지 않는다.
        assertNull(item.version)
    }

    @Test
    fun `DB 금액 자릿수 밖의 값과 음수를 생성 전에 거절한다`() {
        for (amount in listOf("-1", "1.001", "100000000000000000")) {
            assertThrows(LabException::class.java) {
                Item.create(LabScope.LOCAL, "Alpha", true, BigDecimal(amount))
            }
        }
    }
}
