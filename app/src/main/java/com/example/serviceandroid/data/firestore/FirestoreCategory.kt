package com.example.serviceandroid.data.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreCategory(
    @DocumentId
    val id: String = "",
    val name: String = "",
)
