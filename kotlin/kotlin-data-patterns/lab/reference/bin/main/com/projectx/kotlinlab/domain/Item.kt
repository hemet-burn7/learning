package com.projectx.kotlinlab.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "item")
class Item private constructor(scope: LabScope, name: String, active: Boolean, amount: BigDecimal?) {
    @field:Id
    @field:Column(name = "item_id", nullable = false)
    var id: UUID = UUID.randomUUID()
        protected set

    @field:Column(nullable = false, length = 64)
    var tenantId: String = scope.tenantId
        protected set

    @field:Column(nullable = false, length = 64)
    var hospitalCode: String = scope.hospitalCode
        protected set

    @field:Column(nullable = false, length = 120)
    var name: String = validatedName(name)
        protected set

    @field:Column(nullable = false)
    var active: Boolean = active
        protected set

    @field:Column(precision = 19, scale = 2)
    var amount: BigDecimal? = validatedAmount(amount)
        protected set

    // null은 아직 저장하지 않은 Entity를 뜻한다. flush 후 Hibernate가 값을 채운다.
    @field:Version
    var version: Long? = null
        protected set

    fun changeName(name: String) {
        this.name = validatedName(name)
    }

    companion object {
        fun create(scope: LabScope, name: String, active: Boolean, amount: BigDecimal?): Item =
            Item(scope, name, active, amount)

        private fun validatedName(name: String): String {
            val normalized = name.trim()
            if (normalized.isEmpty() || normalized.length > 120) {
                throw LabException(LabError.INVALID_INPUT, "이름은 공백을 제외하고 1~120자여야 합니다.")
            }
            return normalized
        }

        private fun validatedAmount(amount: BigDecimal?): BigDecimal? {
            if (amount == null) return null
            val normalized = amount.stripTrailingZeros()
            if (amount.signum() < 0 || normalized.scale() > 2 ||
                normalized.precision() - normalized.scale() > 17
            ) {
                throw LabException(LabError.INVALID_INPUT, "금액은 0 이상, 정수 17자리와 소수 2자리 이내여야 합니다.")
            }
            return amount
        }
    }
}
