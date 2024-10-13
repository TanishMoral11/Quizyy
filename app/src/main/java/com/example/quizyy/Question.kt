package com.example.quizyy

import kotlinx.serialization.Serializable


@Serializable
data class Question(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)
