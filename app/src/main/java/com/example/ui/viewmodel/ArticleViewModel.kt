package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Article
import com.example.data.network.GsmArenaFeedFetcher
import com.example.data.repository.ArticleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArticleViewModel(private val repository: ArticleRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val newsArticles: StateFlow<List<Article>> = repository.newsArticles
        .combine(searchQuery) { articles, query ->
            filterArticles(articles, query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reviewArticles: StateFlow<List<Article>> = repository.reviewArticles
        .combine(searchQuery) { articles, query ->
            filterArticles(articles, query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkedArticles: StateFlow<List<Article>> = repository.bookmarkedArticles
        .combine(searchQuery) { articles, query ->
            filterArticles(articles, query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initial sync on startup
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            val result = repository.refreshArticles()
            result.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to synchronize, reading offline."
            }
            _isRefreshing.value = false
        }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch {
            repository.setBookmark(article.link, !article.isBookmarked)
        }
    }

    fun markAsRead(article: Article) {
        viewModelScope.launch {
            repository.markAsRead(article.link)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    private fun filterArticles(articles: List<Article>, query: String): List<Article> {
        if (query.isBlank()) return articles
        return articles.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(context)
                    val fetcher = GsmArenaFeedFetcher()
                    val repo = ArticleRepository(db.articleDao(), fetcher)
                    return ArticleViewModel(repo) as T
                }
            }
        }
    }
}
