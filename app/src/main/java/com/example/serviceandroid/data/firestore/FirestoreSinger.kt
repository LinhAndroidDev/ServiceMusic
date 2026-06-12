package com.example.serviceandroid.data.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreSinger(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val description: String = "",
)
