package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.application.queries.ItemFilter
import com.projectx.kotlinlab.application.queries.ItemPage
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.infrastructure.queries.ItemQueries
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/lab/items")
class ItemQueryController(private val queries: ItemQueries) {
    @GetMapping
    fun find(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) minimum: BigDecimal?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ItemPage = queries.find(LabScope.LOCAL, ItemFilter(active, minimum, keyword, page, size))

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): ItemView = queries.findById(LabScope.LOCAL, id)
        ?: throw LabException(LabError.ITEM_NOT_FOUND, "항목을 찾을 수 없습니다.")
}
