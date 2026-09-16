package com.example.serviceandroid.model

enum class TopicType {
    NEW_CHART,
    TOP_100,
    CATEGORY,
    SEE_ALL,
}

data class Topic(
    val icon: Int? = null,
    val topic: String? = null,
    val color: Int? = null,
    val type: TopicType = TopicType.SEE_ALL,
    val categoryId: String = "",
)
