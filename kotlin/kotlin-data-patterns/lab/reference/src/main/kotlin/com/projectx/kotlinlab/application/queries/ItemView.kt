package com.projectx.kotlinlab.application.queries

import com.projectx.kotlinlab.domain.Item
import java.math.BigDecimal
import java.util.UUID

data class ItemView(
    val id: UUID,
    val name: String,
    val active: Boolean,
    val amount: BigDecimal?,
    val version: Long,
) {
    companion object {
        // 저장 핸들러에서만 사용한다. 조회 경로는 QueryDSL이 생성자로 직접 바인딩한다.
        fun from(item: Item): ItemView = ItemView(
            id = item.id,
            name = item.name,
            active = item.active,
            amount = item.amount,
            version = checkNotNull(item.version) { "flush 후 확정된 버전이 필요합니다." },
        )
    }
}
