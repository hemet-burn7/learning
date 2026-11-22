package com.projectx.kotlinlab.presentation

import com.projectx.kotlinlab.infrastructure.eventbus.InMemoryReceivedItemStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/lab/received-events")
class EventObservationController(
    private val receivedItems: InMemoryReceivedItemStore,
    @param:Value("\${lab.kafka.enabled:false}") private val kafkaEnabled: Boolean,
) {
    @GetMapping
    fun find(): ReceivedEventsView = ReceivedEventsView(kafkaEnabled, receivedItems.snapshot())
}
