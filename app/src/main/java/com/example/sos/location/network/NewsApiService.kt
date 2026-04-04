package com.example.sos.location.network

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {

    @GET("v2/everything")
    suspend fun getCrimeNews(
        @Query("q")       query: String,
        @Query("from")    from: String,
        @Query("sortBy")  sortBy: String = "publishedAt",
        @Query("language") language: String = "en",
        @Query("apiKey")  apiKey: String
    ): NewsResponse
}

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val title: String?,
    val description: String?,
    val publishedAt: String?,
    val source: ArticleSource?
)

data class ArticleSource(
    val name: String?
)