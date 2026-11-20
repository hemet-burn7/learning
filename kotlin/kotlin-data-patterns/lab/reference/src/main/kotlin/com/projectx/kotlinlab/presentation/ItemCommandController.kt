package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.application.commands.CreateItemCommand
import com.projectx.kotlinlab.application.commands.CreateItemCommandHandler
import com.projectx.kotlinlab.application.commands.RenameItemCommand
import com.projectx.kotlinlab.application.commands.RenameItemCommandHandler
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.LabScope
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/lab/items")
class ItemCommandController(
    private val createHandler: CreateItemCommandHandler,
    private val renameHandler: RenameItemCommandHandler,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateItemRequest): ResponseEntity<ItemView> {
        val view = createHandler.handle(LabScope.LOCAL, CreateItemCommand(request.name, request.active, request.amount))
        return ResponseEntity.created(URI.create("/lab/items/${view.id}")).body(view)
    }

    @PatchMapping("/{id}/name")
    fun rename(@PathVariable id: UUID, @Valid @RequestBody request: RenameItemRequest): ItemView =
        renameHandler.handle(
            LabScope.LOCAL,
            RenameItemCommand(id, request.name, request.expectedVersion, request.failAfterFlush),
        )
}
