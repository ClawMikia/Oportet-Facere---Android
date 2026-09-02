package com.reqsync.app.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Item animator for the checklist's expand/collapse. Instead of translating sub rows
 * (which slides them over the parent header), it animates each sub row's HEIGHT from 0
 * to full (expand) or full to 0 (collapse). Because height participates in layout, the
 * revealed rows push content downward and never overlap their parent header.
 */
class VerticalSlideItemAnimator : DefaultItemAnimator() {

    private val running = mutableMapOf<View, Pair<RecyclerView.ViewHolder, Boolean>>()

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        val target = view.height
        running[view] = holder to true

        val lp = view.layoutParams
        lp.height = 1
        view.layoutParams = lp

        val anim = ValueAnimator.ofInt(1, target.coerceAtLeast(1)).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val h = animatedValue as Int
                val p = view.layoutParams
                p.height = h
                view.layoutParams = p
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    dispatchFinished(view, holder, true)
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    dispatchFinished(view, holder, true)
                }
            })
        }
        anim.start()
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        val start = view.height.coerceAtLeast(1)
        running[view] = holder to false

        val anim = ValueAnimator.ofInt(start, 0).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val h = animatedValue as Int
                val p = view.layoutParams
                p.height = h
                view.layoutParams = p
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    dispatchFinished(view, holder, false)
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    dispatchFinished(view, holder, false)
                }
            })
        }
        anim.start()
        return true
    }

    // Idempotent so an add/remove is always dispatched exactly once, even if the
    // animation is cancelled, so no item view ever lingers stuck in the RecyclerView.
    private fun resetHeight(view: View) {
        val p = view.layoutParams
        p.height = ViewGroup.LayoutParams.WRAP_CONTENT
        view.layoutParams = p
    }

    private fun dispatchFinished(view: View, holder: RecyclerView.ViewHolder, isAdd: Boolean) {
        if (running.remove(view) == null) return
        resetHeight(view)
        if (isAdd) dispatchAddFinished(holder) else dispatchRemoveFinished(holder)
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ): Boolean {
        return false
    }

    override fun animateChange(
        holder: RecyclerView.ViewHolder,
        oldHolder: RecyclerView.ViewHolder?,
        fromX: Int, fromY: Int, toX: Int, toY: Int
    ): Boolean {
        return false
    }

    override fun isRunning(): Boolean = running.isNotEmpty()

    override fun endAnimations() {
        running.toMap().forEach { (view, pair) -> dispatchFinished(view, pair.first, pair.second) }
        running.clear()
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        running[item.itemView]?.let { (holder, isAdd) -> dispatchFinished(item.itemView, holder, isAdd) }
    }
}
