package com.reqsync.app.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.reqsync.app.adapters.ChecklistAdapter

/**
 * Swipe-left callback that reveals a neon-red delete action behind
 * [ChecklistAdapter.RequirementRow] items. Category headers are excluded.
 */
class SwipeToDeleteCallback(
    context: Context,
    private val onDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteBackground = ColorDrawable(Color.parseColor("#1AFF3D3D"))
    private val deleteTextPaint = Paint().apply {
        color = Color.parseColor("#FF3D3D")
        textSize = 42f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#FF3D3D")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Only allow swipe on item rows (not category headers)
        if (viewHolder.itemViewType != 1) return 0
        return super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false // no drag-and-drop

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onDelete(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val swipeWidth = dX.coerceAtMost(0f) // only left swipe

        // Draw background
        deleteBackground.setBounds(
            (itemView.right + swipeWidth).toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        deleteBackground.draw(c)

        // Draw "DELETE" label when swiped enough
        if (swipeWidth < -150f) {
            val text = "DELETE"
            val textWidth = deleteTextPaint.measureText(text)
            val textX = itemView.right - textWidth - 60f
            val textY = itemView.top + (itemView.height / 2f) + (deleteTextPaint.textSize / 3f)
            c.drawText(text, textX, textY, deleteTextPaint)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
