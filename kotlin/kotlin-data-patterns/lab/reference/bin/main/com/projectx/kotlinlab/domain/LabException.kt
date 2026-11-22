package com.projectx.kotlinlab.domain

class LabException(val error: LabError, message: String) : RuntimeException(message)
