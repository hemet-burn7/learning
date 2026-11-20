package com.projectx.kotlinlab.infrastructure.eventbus

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

// 실습용 단일 브로커 토픽이다. 실제 서비스는 공통 이벤트 라이브러리의 배선을 사용한다.
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "lab.kafka", name = ["enabled"], havingValue = "true")
class LabKafkaTopicConfiguration {
    @Bean
    fun labItemChangedTopic(@Value("\${lab.kafka.topic}") topic: String): NewTopic =
        TopicBuilder.name(topic).partitions(1).replicas(1).build()
}
