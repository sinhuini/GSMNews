package com.example.data.local

import androidx.room.*
import com.example.data.model.Article
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY savedAt DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isReview = :isReview ORDER BY savedAt DESC")
    fun getArticlesByType(isReview: Boolean): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY savedAt DESC")
    fun getBookmarkedArticles(): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<Article>)

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE link = :link")
    suspend fun updateBookmarkStatus(link: String, isBookmarked: Boolean)

    @Query("UPDATE articles SET isRead = :isRead WHERE link = :link")
    suspend fun updateReadStatus(link: String, isRead: Boolean)

    @Query("DELETE FROM articles WHERE isBookmarked = 0")
    suspend fun clearUnbookmarkedArticles()
}
