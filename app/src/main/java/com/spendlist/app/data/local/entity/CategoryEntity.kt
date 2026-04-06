package com.spendlist.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "name_res_key")
    val nameResKey: String?,
    @ColumnInfo(name = "icon_name")
    val iconName: String,
    val color: Long,
    @ColumnInfo(name = "is_preset")
    val isPreset: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
