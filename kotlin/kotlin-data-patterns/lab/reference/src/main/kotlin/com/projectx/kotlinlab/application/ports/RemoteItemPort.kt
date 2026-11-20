package com.projectx.kotlinlab.application.ports

interface RemoteItemPort {
    fun find(externalId: String): RemoteItemSnapshot
}
