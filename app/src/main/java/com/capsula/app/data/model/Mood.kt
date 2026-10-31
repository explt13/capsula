package com.capsula.app.data.model

// Модель данных для настроения пользователя. Содержит эмодзи, метку и числовой балл настроения.
enum class Mood(val emoji: String, val label: String, val score: Int) {
    GREAT("😄", "Great", 5),
    GOOD("🙂", "Good", 4),
    OKAY("😐", "Okay", 3),
    ANXIOUS("😟", "Anxious", 2),
    LOW("😔", "Low", 1),
}
