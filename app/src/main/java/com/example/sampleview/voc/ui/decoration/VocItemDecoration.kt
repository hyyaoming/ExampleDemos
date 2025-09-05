package com.example.sampleview.voc.ui.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 间距装饰
 *
 * @param spanCount 列数
 * @param spacing 间距
 */
class VocItemDecoration(private val spanCount: Int, private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount
        val column = position % spanCount
        val row = position / spanCount
        val rowCount = (itemCount + spanCount - 1) / spanCount

        outRect.left = if (column == 0) 0 else spacing
        outRect.right = if (column == spanCount - 1) 0 else spacing
        outRect.top = if (row == 0) 0 else spacing
        outRect.bottom = if (row == rowCount - 1) 0 else spacing
    }
}