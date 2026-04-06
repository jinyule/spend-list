package com.spendlist.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val nameResKey: String? = null,
    val iconName: String,
    val color: Long,
    val isPreset: Boolean = false,
    val sortOrder: Int = 0
)
