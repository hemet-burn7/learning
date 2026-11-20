package com.projectx.kotlinlab.infrastructure.eventbus

import com.projectx.kotlinlab.application.events.ItemChangedEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

// 실습용 관찰자다. 실무의 inbox 적재·업무 DB 반영·재처리 보장을 구현하지 않는다.
@Component
@ConditionalOnProperty(prefix = "lab.kafka", name = ["enabled"], havingValue = "true")
class ItemChangedKafkaReceiver(
    private val mapper: ObjectMapper,
    private val receivedItems: InMemoryReceivedItemStore,
) {
    @KafkaListener(topics = ["\${lab.kafka.topic}"])
    fun receive(json: String) {
        val event = mapper.readValue(json, ItemChangedEvent::class.java)
        receivedItems.accept(event)
    }
}
