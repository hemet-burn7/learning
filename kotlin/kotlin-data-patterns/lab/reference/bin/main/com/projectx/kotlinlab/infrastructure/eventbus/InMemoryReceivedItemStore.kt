package com.projectx.kotlinlab.infrastructure.eventbus

import com.projectx.kotlinlab.application.events.ItemChangedEvent
import com.projectx.kotlinlab.domain.LabScope
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryReceivedItemStore {
    private val items = ConcurrentHashMap<UUID, ReceivedItemState>()

    fun accept(event: ItemChangedEvent): Boolean {
        // 다른 범위의 이벤트는 이 실습 앱의 관찰 상태에 넣지 않는다.
        if (event.tenantId != LabScope.LOCAL.tenantId || event.hospitalCode != LabScope.LOCAL.hospitalCode) {
            return false
        }
        require(event.sourceVersion >= 0) { "원천 버전은 0 이상이어야 합니다." }
        require(event.name.isNotBlank()) { "수신한 이름은 비어 있을 수 없습니다." }

        var applied = false
        // 같은 항목은 버전 비교와 교체를 한 번의 compute 안에서 수행한다.
        items.compute(event.itemId) { _, current ->
            if (current != null && event.sourceVersion <= current.sourceVersion) {
                current
            } else {
                applied = true
                ReceivedItemState(
                    event.itemId, event.tenantId, event.hospitalCode, event.name, event.sourceVersion,
                )
            }
        }
        return applied
    }

    fun snapshot(): List<ReceivedItemState> = items.values.sortedBy { it.itemId.toString() }
}
