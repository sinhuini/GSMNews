package com.example.data.network

import android.util.Xml
import com.example.data.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

class GsmArenaFeedFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchFeed(): List<Article> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.gsmarena.com/rss-news-opinions.php3")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val xmlData = response.body?.string() ?: return@withContext emptyList()
                parseRss(xmlData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseRss(xml: String): List<Article> {
        val articles = mutableListOf<Article>()
        val parser = Xml.newPullParser()
        try {
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            var title = ""
            var link = ""
            var description = ""
            var pubDate = ""
            var text = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        text = ""
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text.trim()
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            name.equals("title", ignoreCase = true) -> title = text
                            name.equals("link", ignoreCase = true) -> link = text
                            name.equals("description", ignoreCase = true) -> description = text
                            name.equals("pubDate", ignoreCase = true) -> pubDate = text
                            name.equals("item", ignoreCase = true) -> {
                                if (link.isNotEmpty() && title.isNotEmpty()) {
                                    val imageUrl = extractImageUrl(description)
                                    val isReview = link.contains("/review-") || link.contains("-review-") || title.contains("review", ignoreCase = true)
                                    articles.add(
                                        Article(
                                            link = link,
                                            title = title,
                                            description = cleanHtmlDescription(description),
                                            pubDate = pubDate,
                                            imageUrl = imageUrl,
                                            isReview = isReview
                                        )
                                    )
                                }
                                title = ""
                                link = ""
                                description = ""
                                pubDate = ""
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return articles
    }

    private fun extractImageUrl(html: String): String? {
        val regex = """<img[^>]+src=["'](https?://[^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = regex.find(html)
        return matchResult?.groupValues?.get(1)?.replace("&amp;", "&")
    }

    private fun cleanHtmlDescription(html: String): String {
        var cleaned = html.replace("""<img[^>]+>""".toRegex(), "")
        cleaned = cleaned.replace("""<br\s*/?>""".toRegex(), "\n")
        cleaned = cleaned.replace("""<[^>]+>""".toRegex(), "")
        return cleaned.trim()
    }
}
