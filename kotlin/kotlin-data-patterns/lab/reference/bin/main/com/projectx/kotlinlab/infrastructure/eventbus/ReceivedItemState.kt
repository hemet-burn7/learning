package com.projectx.kotlinlab.infrastructure.eventbus

import java.util.UUID

// 수신 내용을 관찰하기 위한 메모리 값이다. JPA Entity나 영속 inbox 행이 아니다.
data class ReceivedItemState(
    val itemId: UUID,
    val tenantId: String,
    val hospitalCode: String,
    val name: String,
    val sourceVersion: Long,
)
