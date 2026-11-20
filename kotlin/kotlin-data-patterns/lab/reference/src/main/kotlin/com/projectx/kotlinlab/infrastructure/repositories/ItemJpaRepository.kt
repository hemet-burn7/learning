package com.projectx.kotlinlab.infrastructure.repositories

import com.projectx.kotlinlab.domain.Item
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ItemJpaRepository : JpaRepository<Item, UUID>
