package com.reqsync.app.utils

import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// Date formatting helpers
// ─────────────────────────────────────────────────────────────────────────────
fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000} hr ago"
        diff < 604_800_000L -> "${diff / 86_400_000} days ago"
        else -> this.toFormattedDate()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Color helpers
// ─────────────────────────────────────────────────────────────────────────────
fun String.toColorInt(): Int = try {
    Color.parseColor(this)
} catch (e: Exception) {
    Color.parseColor("#00F5FF")
}

// ─────────────────────────────────────────────────────────────────────────────
// XP / Level math
// ─────────────────────────────────────────────────────────────────────────────
object XpUtils {
    fun levelFromXp(xp: Int): Int = (1 + sqrt(xp / 100.0)).toInt().coerceAtLeast(1)

    fun xpForLevel(level: Int): Int = (level - 1) * (level - 1) * 100

    fun xpForNextLevel(level: Int): Int = xpForLevel(level + 1)

    fun progressPercent(totalXp: Int): Float {
        val level = levelFromXp(totalXp)
        val current = totalXp - xpForLevel(level)
        val needed = xpForNextLevel(level) - xpForLevel(level)
        return if (needed == 0) 1f else (current.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Number formatting
// ─────────────────────────────────────────────────────────────────────────────
fun Int.toXpString(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000}M XP"
    this >= 1_000 -> "${this / 1_000}K XP"
    else -> "$this XP"
}
