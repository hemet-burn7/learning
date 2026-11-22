package com.projectx.kotlinlab.application.queries

import com.projectx.kotlinlab.domain.LabException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ItemFilterTest {
    @Test
    fun `허용 범위 밖의 페이지 입력을 거절한다`() {
        assertThrows(LabException::class.java) { ItemFilter(page = -1) }
        assertThrows(LabException::class.java) { ItemFilter(size = 0) }
        assertThrows(LabException::class.java) { ItemFilter(size = 101) }
    }
}
