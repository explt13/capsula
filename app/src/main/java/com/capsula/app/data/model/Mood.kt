package com.capsula.app.data.model

/**
 * How the user feels at a given moment.
 *
 * [score] gives the moods an order, which lets us compare the mood at the time a capsule
 * was sealed with the mood at the time it was opened ("then vs now").
 */
enum class Mood(val emoji: String, val label: String, val score: Int) {
    GREAT("😄", "Great", 5),
    GOOD("🙂", "Good", 4),
    OKAY("😐", "Okay", 3),
    ANXIOUS("😟", "Anxious", 2),
    LOW("😔", "Low", 1),
}
