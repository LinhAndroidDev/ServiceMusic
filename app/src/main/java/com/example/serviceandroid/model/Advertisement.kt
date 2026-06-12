package com.example.serviceandroid.model

import com.example.serviceandroid.data.firestore.FirestoreAdvertisement

data class Advertisement(
    val id: String = "",
    val image: String = "",
    val update: String = "",
    val detail: String = "",
)

fun FirestoreAdvertisement.toDomainAdvertisement(): Advertisement = Advertisement(
    id = id,
    image = image,
    update = update,
    detail = detail,
)
