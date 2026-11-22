package com.projectx.kotlinlab.application.commands

import java.math.BigDecimal

data class CreateItemCommand(val name: String, val active: Boolean = true, val amount: BigDecimal? = null)
