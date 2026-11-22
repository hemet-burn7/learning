package com.projectx.kotlinlab.application.services

import com.projectx.kotlinlab.application.commands.RenameItemCommand
import com.projectx.kotlinlab.application.commands.RenameItemCommandHandler
import com.projectx.kotlinlab.application.ports.RemoteItemPort
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.infrastructure.queries.ItemQueries
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RefreshItemNameService(
    private val remoteItems: RemoteItemPort,
    private val renameHandler: RenameItemCommandHandler,
    private val queries: ItemQueries,
) {
    // 이 흐름에는 트랜잭션을 붙이지 않는다. HTTP가 끝난 뒤 별도 핸들러가 쓰기를 시작한다.
    fun refresh(scope: LabScope, localId: UUID, externalId: String, expectedVersion: Long): ItemView {
        val remote = remoteItems.find(externalId)
        val name = remote.name ?: return queries.findById(scope, localId)
            ?: throw LabException(LabError.ITEM_NOT_FOUND, "항목을 찾을 수 없습니다.")

        // 빈 문자열을 null로 바꾸지 않는다. 도메인 규칙이 변경을 거절하도록 전달한다.
        return renameHandler.handle(
            scope,
            RenameItemCommand(id = localId, name = name, expectedVersion = expectedVersion),
        )
    }
}
