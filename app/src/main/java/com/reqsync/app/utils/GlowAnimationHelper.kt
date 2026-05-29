package com.reqsync.app.utils

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd

/**
 * Runtime glow / pulse animation helpers.
 * Applied to views to create a cyberpunk holographic feel without Lottie JSON files.
 */
object GlowAnimationHelper {

    /**
     * Animate a progress bar filling from 0 to [targetPercent] over [durationMs].
     */
    fun animateProgressBar(
        progressBar: ProgressBar,
        targetPercent: Int,
        durationMs: Long = 800L
    ) {
        val animator = ValueAnimator.ofInt(0, targetPercent).apply {
            duration = durationMs
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { progressBar.progress = it.animatedValue as Int }
        }
        animator.start()
    }

    /**
     * Pulse-glow a neon text view (alpha oscillation).
     * Call once; repeats indefinitely until [cancelGlow] is called.
     */
    fun startPulseGlow(view: View, minAlpha: Float = 0.4f, maxAlpha: Float = 1.0f): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", maxAlpha, minAlpha).apply {
            duration = 900L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun cancelGlow(animator: ObjectAnimator?) {
        animator?.cancel()
    }

    /**
     * Colour-flash a [TextView] with a single XP-reward burst.
     * white → neon cyan → original colour
     */
    fun flashXpColor(textView: TextView, originalColor: Int = Color.WHITE) {
        val neonCyan = Color.parseColor("#00F5FF")
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), originalColor, neonCyan, originalColor).apply {
            duration = 600L
            addUpdateListener { textView.setTextColor(it.animatedValue as Int) }
        }
        anim.start()
    }

    /**
     * Scale-bounce a view when a task is completed.
     */
    fun completionBounce(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.18f, 1f).apply {
            duration = 350L
            interpolator = android.view.animation.OvershootInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.18f, 1f).apply {
            duration = 350L
            interpolator = android.view.animation.OvershootInterpolator()
        }
        android.animation.AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    /**
     * Animate a tint colour change on a ProgressBar from one hex colour to another.
     */
    fun animateTint(progressBar: ProgressBar, fromHex: String, toHex: String, durationMs: Long = 500L) {
        val from = fromHex.toColorInt()
        val to = toHex.toColorInt()
        ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
            duration = durationMs
            addUpdateListener {
                progressBar.progressTintList = ColorStateList.valueOf(it.animatedValue as Int)
            }
            start()
        }
    }

    /**
     * Fade a view in from invisible, optionally translating upward (for toasts / banners).
     */
    fun fadeInUp(view: View, translatePx: Float = 40f, durationMs: Long = 400L, onEnd: (() -> Unit)? = null) {
        view.alpha = 0f
        view.translationY = translatePx
        view.visibility = View.VISIBLE
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply { duration = durationMs }
        val transAnim = ObjectAnimator.ofFloat(view, "translationY", translatePx, 0f).apply {
            duration = durationMs
            interpolator = android.view.animation.DecelerateInterpolator()
        }
        android.animation.AnimatorSet().apply {
            playTogether(alphaAnim, transAnim)
            doOnEnd { onEnd?.invoke() }
            start()
        }
    }

    /**
     * Fade a view out and hide it when done.
     */
    fun fadeOut(view: View, durationMs: Long = 300L, onEnd: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            duration = durationMs
            doOnEnd {
                view.visibility = View.GONE
                view.alpha = 1f
                onEnd?.invoke()
            }
            start()
        }
    }
}
