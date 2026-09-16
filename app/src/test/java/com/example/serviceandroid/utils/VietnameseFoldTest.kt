package com.example.serviceandroid.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VietnameseFoldTest {

    @Test
    fun fold_stripsAccentsAndLowercases() {
        assertEquals("son tung", VietnameseFold.fold("Sơn Tùng"))
        assertEquals("de xuat", VietnameseFold.fold("Đề xuất"))
        assertEquals("nghe nhac", VietnameseFold.fold("  Nghe nhạc  "))
    }

    @Test
    fun matches_findsUnsignedQueryInAccentedText() {
        assertTrue(VietnameseFold.matches("Sơn Tùng M-TP", "son tung"))
        assertTrue(VietnameseFold.matches("Đề xuất cho bạn", "de xuat"))
        assertFalse(VietnameseFold.matches("Sơn Tùng", "jack"))
    }

    @Test
    fun contains_usesFoldedNeedle() {
        val folded = VietnameseFold.fold("son tung")
        assertTrue(VietnameseFold.contains("Chúng ta của Sơn Tùng", folded))
        assertFalse(VietnameseFold.contains("Jack", folded))
    }
}
