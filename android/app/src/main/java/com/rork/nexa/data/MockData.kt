package com.rork.nexa.data

import com.rork.nexa.models.SafetyAlert
import com.rork.nexa.models.SafetyLevel
import com.rork.nexa.models.SuggestedFriend

object MockData {
    val suggestedFriends = listOf(
        SuggestedFriend("Maya", "M", 0xFFFF6BA8),
        SuggestedFriend("Jordan", "J", 0xFF34E5C8),
        SuggestedFriend("Sam", "S", 0xFF7C5CFF),
        SuggestedFriend("Aria", "A", 0xFFFFB547),
        SuggestedFriend("Liam", "L", 0xFF53D593),
        SuggestedFriend("Noor", "N", 0xFFFF8A8A),
    )

    val sampleAlerts = listOf(
        SafetyAlert(
            id = "a1",
            title = "Calm week",
            description = "No risky messages spotted in the last 7 days.",
            level = SafetyLevel.Safe,
            timeAgo = "this week",
            source = "Shield",
        ),
        SafetyAlert(
            id = "a2",
            title = "Heads up on a new contact",
            description = "Someone outside your circle messaged you. We held it for review.",
            level = SafetyLevel.Watch,
            timeAgo = "2d ago",
            source = "Request inbox",
        ),
    )
}

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
