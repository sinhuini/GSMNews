package com.example.data.repository

import com.example.data.local.ArticleDao
import com.example.data.model.Article
import com.example.data.network.GsmArenaFeedFetcher
import kotlinx.coroutines.flow.Flow

class ArticleRepository(
    private val articleDao: ArticleDao,
    private val feedFetcher: GsmArenaFeedFetcher
) {
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()
    val newsArticles: Flow<List<Article>> = articleDao.getArticlesByType(isReview = false)
    val reviewArticles: Flow<List<Article>> = articleDao.getArticlesByType(isReview = true)
    val bookmarkedArticles: Flow<List<Article>> = articleDao.getBookmarkedArticles()

    suspend fun refreshArticles(): Result<Unit> {
        return try {
            val fetched = feedFetcher.fetchFeed()
            if (fetched.isNotEmpty()) {
                articleDao.insertArticles(fetched)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setBookmark(link: String, isBookmarked: Boolean) {
        articleDao.updateBookmarkStatus(link, isBookmarked)
    }

    suspend fun markAsRead(link: String) {
        articleDao.updateReadStatus(link, true)
    }

    suspend fun clearCache() {
        articleDao.clearUnbookmarkedArticles()
    }
}
