package com.silema.app.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 紧急联系人实体。
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val phone: String,
    val name: String,
)
