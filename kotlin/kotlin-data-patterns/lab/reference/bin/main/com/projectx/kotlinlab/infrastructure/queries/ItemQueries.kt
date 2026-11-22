package com.projectx.kotlinlab.infrastructure.queries

import com.projectx.kotlinlab.application.queries.ItemFilter
import com.projectx.kotlinlab.application.queries.ItemPage
import com.projectx.kotlinlab.application.queries.ItemView
import com.projectx.kotlinlab.domain.LabScope
import com.projectx.kotlinlab.domain.QItem
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Transactional(readOnly = true)
class ItemQueries(private val queryFactory: JPAQueryFactory) {
    private val item = QItem.item

    fun find(scope: LabScope, filter: ItemFilter): ItemPage {
        val condition = condition(scope, filter)
        val rows = queryFactory.select(viewProjection()).from(item)
            .where(condition)
            .orderBy(item.name.asc(), item.id.asc())
            .offset(filter.page.toLong() * filter.size)
            .limit(filter.size.toLong())
            .fetch()
        // 목록과 같은 후보 집합을 센다. count에는 offset/limit를 적용하지 않는다.
        val total = queryFactory.select(item.id.count()).from(item).where(condition).fetchOne() ?: 0L
        return ItemPage(rows, total, filter.page, filter.size)
    }

    fun findById(scope: LabScope, id: UUID): ItemView? = queryFactory.select(viewProjection()).from(item)
        .where(condition(scope, ItemFilter()), item.id.eq(id))
        .fetchOne()

    private fun condition(scope: LabScope, filter: ItemFilter): BooleanBuilder {
        val condition = BooleanBuilder(item.tenantId.eq(scope.tenantId))
            .and(item.hospitalCode.eq(scope.hospitalCode))
        filter.active?.let { condition.and(item.active.eq(it)) }
        filter.minimum?.let { condition.and(item.amount.goe(it)) }
        filter.keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { condition.and(item.name.containsIgnoreCase(it)) }
        return condition
    }

    private fun viewProjection() = Projections.constructor(
        ItemView::class.java, item.id, item.name, item.active, item.amount, item.version,
    )
}
