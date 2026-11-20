package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    val isSeries: Boolean,
    val title: String,
    val posterUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
