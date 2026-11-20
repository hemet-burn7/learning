package com.projectx.kotlinlab.infrastructure.repositories

import com.projectx.kotlinlab.domain.Item
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.domain.QItem
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ItemRepository(private val queryFactory: JPAQueryFactory, private val jpa: ItemJpaRepository) {
    private val item = QItem.item

    // 쓰기 대상을 찾을 때도 id와 함께 tenant·hospital 범위를 검사한다.
    fun find(scope: LabScope, id: UUID): Item? = queryFactory.selectFrom(item)
        .where(item.id.eq(id), item.tenantId.eq(scope.tenantId), item.hospitalCode.eq(scope.hospitalCode))
        .fetchOne()

    fun saveAndFlush(item: Item): Item = jpa.saveAndFlush(item)
}
