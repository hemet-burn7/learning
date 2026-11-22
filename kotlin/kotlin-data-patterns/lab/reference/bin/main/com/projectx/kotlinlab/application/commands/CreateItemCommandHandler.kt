package com.projectx.kotlinlab.application.commands

import com.projectx.kotlinlab.application.events.ItemChangedEvent
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.Item
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.infrastructure.repositories.ItemRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateItemCommandHandler(
    private val repository: ItemRepository,
    private val events: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(scope: LabScope, command: CreateItemCommand): ItemView {
        val item = Item.create(scope, command.name, command.active, command.amount)
        val saved = repository.saveAndFlush(item)
        val view = ItemView.from(saved)
        events.publishEvent(ItemChangedEvent(saved.id, saved.tenantId, saved.hospitalCode, saved.name, view.version))
        return view
    }
}
