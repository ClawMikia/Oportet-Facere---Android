package com.reqsync.app.utils

import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.reqsync.app.R

// ─────────────────────────────────────────────────────────────────────────────
// Fling-scroll guard so fast scrolling doesn't re-run entrance animations.
// Only skip when the recycler is flinging hard; normal (re)binds animate.
// ─────────────────────────────────────────────────────────────────────────────
private fun RecyclerView?.isFlinging(): Boolean =
    this != null && scrollState == RecyclerView.SCROLL_STATE_SETTLING

// ─────────────────────────────────────────────────────────────────────────────
// Slide each item in from the right with a subtle scale/fade and a small
// staggered delay based on position. Plays on every bind, which covers initial
// load, returning to the screen, and expand/collapse reveals.
// ─────────────────────────────────────────────────────────────────────────────
fun View.slideInFromRight(position: Int) {
    val recycler = parent as? RecyclerView
    if (recycler.isFlinging()) return

    val anim = AnimationUtils.loadAnimation(context, R.anim.item_slide_in_entrance).apply {
        startOffset = (position * 40L).coerceAtMost(240L)
    }
    startAnimation(anim)
}

// ─────────────────────────────────────────────────────────────────────────────
// Slide a parent item in moving TO THE RIGHT (enters from the left edge).
// Used for parent category headers.
// ─────────────────────────────────────────────────────────────────────────────
fun View.slideInToRight(position: Int) {
    val recycler = parent as? RecyclerView
    if (recycler.isFlinging()) return

    val anim = AnimationUtils.loadAnimation(context, R.anim.item_slide_in_from_left).apply {
        startOffset = (position * 40L).coerceAtMost(240L)
    }
    startAnimation(anim)
}
