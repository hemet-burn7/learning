package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 외부 시스템 역할을 대신하는 학습 전용 HTTP 응답이다. DB를 읽거나 변경하지 않는다.
@RestController
@RequestMapping("/lab/remote/items")
class RemoteItemController {
    @GetMapping("/{externalId}")
    fun find(@PathVariable externalId: String): RemoteItemView = when (externalId) {
        "alice" -> RemoteItemView(name = "Alice")
        "unchanged" -> RemoteItemView(name = null)
        "blank" -> RemoteItemView(name = "")
        else -> throw LabException(LabError.ITEM_NOT_FOUND, "실습용 외부 항목이 없습니다.")
    }
}
