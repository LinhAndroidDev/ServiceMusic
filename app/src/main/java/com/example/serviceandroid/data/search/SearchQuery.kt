package com.example.serviceandroid.data.search

data class SearchQuery(
    val query: String,
    val normalizedQuery: String,
    val lastSearchedAt: Long = 0L,
)
