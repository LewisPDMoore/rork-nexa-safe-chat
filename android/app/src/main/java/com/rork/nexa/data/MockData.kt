package com.rork.nexa.data

fun analyseMessage(text: String): MessageRisk {
    val lower = text.lowercase()
    val red = listOf("kill", "die", "hate you", "loser", "stupid", "ugly", "shut up", "idiot", "trash")
    val amber = listOf("annoying", "weird", "whatever", "no one likes")
    return when {
        red.any { lower.contains(it) } -> MessageRisk.High
        amber.any { lower.contains(it) } -> MessageRisk.Medium
        else -> MessageRisk.Low
    }
}

fun softerSuggestion(text: String): String? {
    val lower = text.lowercase()
    return when {
        lower.contains("loser") || lower.contains("idiot") || lower.contains("stupid") ->
            "Hey, that's not okay — could you say what's actually bothering you?"
        lower.contains("hate you") -> "I'm really frustrated right now — can we talk later?"
        lower.contains("shut up") -> "Can we pause this for a sec?"
        lower.contains("ugly") || lower.contains("trash") -> "I don't think that's fair to say."
        else -> null
    }
}

enum class MessageRisk { Low, Medium, High }
