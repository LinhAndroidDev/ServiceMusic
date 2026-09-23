package com.example.serviceandroid.utils

import java.text.Normalizer

object VietnameseFold {
    private val COMBINING_MARKS = "\\p{M}+".toRegex()
    private val EXTRA_SPACES = "\\s+".toRegex()

    fun fold(input: String): String {
        val nfd = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
        return nfd.replace(COMBINING_MARKS, "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
            .lowercase()
            .replace(EXTRA_SPACES, " ")
            .trim()
    }

    fun contains(haystack: String, foldedNeedle: String): Boolean {
        if (foldedNeedle.isBlank()) return false
        return fold(haystack).contains(foldedNeedle)
    }

    fun matches(haystack: String, query: String): Boolean {
        val foldedQuery = fold(query)
        if (foldedQuery.isBlank()) return false
        return fold(haystack).contains(foldedQuery)
    }
}
