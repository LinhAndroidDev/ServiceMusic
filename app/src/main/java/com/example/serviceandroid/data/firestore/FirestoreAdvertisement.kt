package com.example.serviceandroid.data.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreAdvertisement(
    @DocumentId
    val id: String = "",
    val image: String = "",
    val update: String = "",
    val detail: String = "",
)
