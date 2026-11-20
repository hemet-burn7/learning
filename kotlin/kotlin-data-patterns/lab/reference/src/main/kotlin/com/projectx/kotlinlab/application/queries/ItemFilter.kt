package com.projectx.kotlinlab.application.queries

import com.projectx.kotlinlab.domain.LabError
import com.projectx.kotlinlab.domain.LabException
import java.math.BigDecimal

data class ItemFilter(
    val active: Boolean? = null,
    val minimum: BigDecimal? = null,
    val keyword: String? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    init {
        if (page < 0 || size !in 1..100) {
            throw LabException(LabError.INVALID_INPUT, "page는 0 이상, size는 1~100이어야 합니다.")
        }
    }
}
