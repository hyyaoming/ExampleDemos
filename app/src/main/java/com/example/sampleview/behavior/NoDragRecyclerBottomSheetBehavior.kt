package com.example.sampleview.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class NoDragRecyclerBottomSheetBehavior<V : View>(
    context: Context,
    attrs: AttributeSet
) : BottomSheetBehavior<V>(context, attrs) {

    private var isTouchOnRecyclerView = false

    fun setTouchOnRecyclerView(flag: Boolean) {
        isTouchOnRecyclerView = flag
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        // 拦截 RecyclerView 嵌套滑动
        return if (isTouchOnRecyclerView && target is RecyclerView) {
            false
        } else {
            super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
        }
    }
}

