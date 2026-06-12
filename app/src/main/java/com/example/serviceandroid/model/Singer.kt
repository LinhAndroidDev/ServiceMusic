package com.example.serviceandroid.model

import com.example.serviceandroid.data.firestore.FirestoreSinger

data class Singer(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val description: String,
)

fun FirestoreSinger.toDomainSinger(): Singer = Singer(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    description = description,
)
