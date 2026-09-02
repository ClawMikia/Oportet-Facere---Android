package com.reqsync.app.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom ItemAnimator that adds a slide-out-left animation when items are removed
 * (e.g. filter chip changes, accordion collapse, swipe-to-delete, archiving).
 *
 * For adds, returns false so the existing [slideInFromRight] extension in
 * onBindViewHolder handles the staggered entrance animation.
 */
class SlideItemAnimator : DefaultItemAnimator() {

    private val runningRemovals = mutableMapOf<View, RecyclerView.ViewHolder>()

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        // Let slideInFromRight in onBindViewHolder handle add animations.
        return false
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        runningRemovals[view] = holder
        view.animate()
            .translationX(-view.width.toFloat())
            .alpha(0f)
            .setDuration(280)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = finishRemove(view, holder)
                override fun onAnimationCancel(animation: Animator) = finishRemove(view, holder)
            })
            .start()
        return true
    }

    // Removed items must ALWAYS be dispatched as finished, otherwise the RecyclerView
    // treats the removal as still running (isRunning() stays true) and the stale item
    // view lingers on screen. Finish is idempotent so it runs exactly once per view.
    private fun finishRemove(view: View, holder: RecyclerView.ViewHolder) {
        if (runningRemovals.remove(view) == null) return
        view.animate().cancel()
        view.clearAnimation()
        view.alpha = 1f
        view.translationX = 0f
        dispatchRemoveFinished(holder)
    }

    override fun animateChange(
        holder: RecyclerView.ViewHolder,
        oldHolder: RecyclerView.ViewHolder?,
        fromX: Int, fromY: Int, toX: Int, toY: Int
    ): Boolean {
        return false
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ): Boolean {
        return false
    }

    override fun isRunning(): Boolean = runningRemovals.isNotEmpty()

    override fun endAnimations() {
        runningRemovals.toMap().forEach { (view, holder) -> finishRemove(view, holder) }
        runningRemovals.clear()
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        runningRemovals[item.itemView]?.let { finishRemove(item.itemView, it) }
    }
}
