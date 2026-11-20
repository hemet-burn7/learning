package com.projectx.kotlinlab.application.ports

// null은 변경할 이름이 없다는 뜻이며, 빈 문자열은 도메인에서 거절한다.
data class RemoteItemSnapshot(val name: String?)
