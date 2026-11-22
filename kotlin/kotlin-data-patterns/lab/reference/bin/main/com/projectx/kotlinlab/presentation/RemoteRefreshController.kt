package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.application.services.RefreshItemNameService
import com.projectx.kotlinlab.domain.LabScope
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/lab/items")
class RemoteRefreshController(private val service: RefreshItemNameService) {
    @PostMapping("/{id}/refresh-name")
    fun refresh(@PathVariable id: UUID, @RequestBody request: RefreshItemNameRequest): ItemView =
        service.refresh(LabScope.LOCAL, id, request.externalId, request.expectedVersion)
}
