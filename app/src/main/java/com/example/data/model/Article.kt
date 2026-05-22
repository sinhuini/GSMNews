package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey val link: String,
    val title: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String?,
    val isReview: Boolean,
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
) : Serializable
