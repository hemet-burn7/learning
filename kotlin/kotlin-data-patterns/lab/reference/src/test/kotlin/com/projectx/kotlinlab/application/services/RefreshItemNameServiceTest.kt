package com.projectx.kotlinlab.application.services

import com.projectx.kotlinlab.application.commands.RenameItemCommand
import com.projectx.kotlinlab.application.commands.RenameItemCommandHandler
import com.projectx.kotlinlab.application.ports.RemoteItemPort
import com.projectx.kotlinlab.application.ports.RemoteItemSnapshot
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.infrastructure.queries.ItemQueries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

// 흐름과 입력 전달만 검사한다. JPA commit/rollback이나 HTTP 연결을 검증하는 테스트는 아니다.
class RefreshItemNameServiceTest {
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val scope = LabScope.LOCAL
    private val handler = Mockito.mock(RenameItemCommandHandler::class.java)
    private val queries = Mockito.mock(ItemQueries::class.java)

    @Test
    fun `외부 이름과 화면 버전을 기존 쓰기 핸들러에 전달한다`() {
        val command = RenameItemCommand(id, "Alice", 3)
        val expected = ItemView(id, "Alice", true, null, 4)
        Mockito.`when`(handler.handle(scope, command)).thenReturn(expected)

        val actual = service("Alice").refresh(scope, id, "alice", 3)

        assertEquals(expected, actual)
        Mockito.verify(handler).handle(scope, command)
        Mockito.verifyNoInteractions(queries)
    }

    @Test
    fun `외부 이름이 null이면 쓰기를 호출하지 않고 현재 조회값을 반환한다`() {
        val current = ItemView(id, "Old name", true, null, 3)
        Mockito.`when`(queries.findById(scope, id)).thenReturn(current)

        assertEquals(current, service(null).refresh(scope, id, "unchanged", 1))
        Mockito.verifyNoInteractions(handler)
    }

    @Test
    fun `빈 문자열은 숨기지 않고 쓰기 규칙에 전달하며 거절 오류를 전파한다`() {
        val command = RenameItemCommand(id, "", 3)
        Mockito.`when`(handler.handle(scope, command))
            .thenThrow(LabException(LabError.INVALID_INPUT, "이름은 비어 있을 수 없습니다."))

        assertThrows(LabException::class.java) { service("").refresh(scope, id, "blank", 3) }
        Mockito.verify(handler).handle(scope, command)
        Mockito.verifyNoInteractions(queries)
    }

    private fun service(name: String?): RefreshItemNameService {
        val remote = object : RemoteItemPort {
            override fun find(externalId: String) = RemoteItemSnapshot(name)
        }
        return RefreshItemNameService(remote, handler, queries)
    }
}
