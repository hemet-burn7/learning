package com.projectx.kotlinlab.presentation

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateItemRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    val active: Boolean = true,
    @field:DecimalMin("0.00")
    @field:Digits(integer = 17, fraction = 2)
    val amount: BigDecimal? = null,
)
