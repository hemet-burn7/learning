package com.projectx.kotlinlab.infrastructure.eventbus

import com.projectx.kotlinlab.application.events.ItemChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(prefix = "lab.kafka", name = ["enabled"], havingValue = "true")
class ItemChangedKafkaPublisher(
    private val kafka: KafkaTemplate<String, String>,
    private val mapper: ObjectMapper,
    @param:Value("\${lab.kafka.topic}") private val topic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publish(event: ItemChangedEvent) {
        // DB는 이미 커밋됐다. 발행 실패를 잡아도 DB가 되돌아가는 것은 아니다.
        try {
            val json = mapper.writeValueAsString(event)
            kafka.send(topic, event.itemId.toString(), json).whenComplete { _, failure ->
                if (failure != null) {
                    log.warn("실습 Kafka 발행이 실패했습니다. DB는 이미 커밋됐으며 재전송 기록은 없습니다.")
                }
            }
        } catch (failure: Exception) {
            log.warn("실습 Kafka 발행을 시작하지 못했습니다. DB는 이미 커밋됐으며 재전송 기록은 없습니다.")
        }
    }
}
