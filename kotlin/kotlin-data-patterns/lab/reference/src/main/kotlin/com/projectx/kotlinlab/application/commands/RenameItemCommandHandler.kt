package com.projectx.kotlinlab.application.commands

import com.projectx.kotlinlab.application.events.ItemChangedEvent
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.infrastructure.repositories.ItemRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RenameItemCommandHandler(
    private val repository: ItemRepository,
    private val events: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(scope: LabScope, command: RenameItemCommand): ItemView {
        val item = repository.find(scope, command.id)
            ?: throw LabException(LabError.ITEM_NOT_FOUND, "항목을 찾을 수 없습니다.")
        if (item.version != command.expectedVersion) {
            throw LabException(LabError.VERSION_CONFLICT, "조회한 버전이 오래되었습니다. 다시 조회해 주세요.")
        }
        item.changeName(command.name)
        val saved = repository.saveAndFlush(item)
        val view = ItemView.from(saved)
        events.publishEvent(ItemChangedEvent(saved.id, saved.tenantId, saved.hospitalCode, saved.name, view.version))
        if (command.failAfterFlush) {
            throw LabException(LabError.DEMO_ROLLBACK, "flush 뒤 의도적으로 실패했습니다. 변경은 rollback됩니다.")
        }
        return view
    }
}
