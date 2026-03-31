package com.example.conneqt

// This is your data class — one object = one class the professor teaches
data class ClassModel(
    val id: String,
    val name: String,
    val studentCount: Int,
    val uploadDate: String,
    val emoji: String = "📘",     // default emoji for the card icon
    val isActive: Boolean = true  // controls the green dot
)