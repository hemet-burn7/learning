package com.projectx.kotlinlab.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PingController {
    @GetMapping("/lab/ping")
    fun ping(): Map<String, String> = mapOf("status" to "ok")
}
