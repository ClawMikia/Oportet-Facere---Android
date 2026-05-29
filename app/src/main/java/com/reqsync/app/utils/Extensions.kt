package com.reqsync.app.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.reqsync.app.R
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Date formatting helpers
// ─────────────────────────────────────────────────────────────────────────────
fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault())
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
// View extensions
// ─────────────────────────────────────────────────────────────────────────────
fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.animateIn(context: Context) {
    val anim = AnimationUtils.loadAnimation(context, R.anim.fade_scale_in)
    this.startAnimation(anim)
}

fun View.pulseGlow(context: Context) {
    val anim = AnimationUtils.loadAnimation(context, R.anim.pulse_glow)
    this.startAnimation(anim)
}

// ─────────────────────────────────────────────────────────────────────────────
// Color helpers
// ─────────────────────────────────────────────────────────────────────────────
fun String.toColorInt(): Int = try {
    Color.parseColor(this)
} catch (e: Exception) {
    Color.parseColor("#00F5FF")
}

fun ProgressBar.setGlowColor(hexColor: String) {
    val color = hexColor.toColorInt()
    progressTintList = ColorStateList.valueOf(color)
}

// ─────────────────────────────────────────────────────────────────────────────
// XP / Level math
// ─────────────────────────────────────────────────────────────────────────────
object XpUtils {
    fun levelFromXp(xp: Int): Int = (1 + Math.sqrt(xp / 100.0)).toInt().coerceAtLeast(1)

    fun xpForLevel(level: Int): Int = (level - 1) * (level - 1) * 100

    fun xpForNextLevel(level: Int): Int = xpForLevel(level + 1)

    fun progressPercent(totalXp: Int): Float {
        val level = levelFromXp(totalXp)
        val current = totalXp - xpForLevel(level)
        val needed = xpForNextLevel(level) - xpForLevel(level)
        return if (needed == 0) 1f else (current.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
    }

    fun rankLabel(level: Int): String = when {
        level >= 50 -> "GHOST PROTOCOL"
        level >= 40 -> "SHADOW AGENT"
        level >= 30 -> "CYBER ELITE"
        level >= 25 -> "PHANTOM"
        level >= 20 -> "SPECIALIST"
        level >= 15 -> "OPERATIVE"
        level >= 10 -> "CYBER WARRIOR"
        level >= 7  -> "ENFORCER"
        level >= 5  -> "RISING STAR"
        level >= 3  -> "INITIATE"
        else        -> "RECRUIT"
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

fun Float.toPercentString(): String = "${(this * 100).toInt()}%"
