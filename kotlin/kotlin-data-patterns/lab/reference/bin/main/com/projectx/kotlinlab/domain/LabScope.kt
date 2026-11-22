package com.projectx.kotlinlab.domain

data class LabScope(val tenantId: String, val hospitalCode: String) {
    companion object {
        // localhost 전용 실습값이다. 실제 서비스는 인증에서 얻은 범위를 사용한다.
        val LOCAL = LabScope(tenantId = "T1", hospitalCode = "H1")
    }
}
