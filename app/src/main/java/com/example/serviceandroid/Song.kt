package com.example.serviceandroid

import java.io.Serializable

data class Song(
    val title: String,
    val name: String,
    val avatar: Int,
    val sing: Int,
    val time: Int
): Serializable