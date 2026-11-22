package com.projectx.kotlinlab.application.queries

data class ItemPage(val items: List<ItemView>, val total: Long, val page: Int, val size: Int)
