package com.example.sampleview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.ref.WeakReference

class TwoStageBottomSheetBehavior<V : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BottomSheetBehavior<V>(context, attrs) {


    private var nestedScrollingChildRef: WeakReference<View>? = null

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        val scrollChild = nestedScrollingChildRef?.get()
        if (scrollChild != null && event.actionMasked == MotionEvent.ACTION_MOVE) {
            // 如果 RecyclerView 能向上滚动（即还没到顶部），就不要让 BottomSheet 抢手势
            if (scrollChild.canScrollVertically(-1)) {
                return false
            }
        }
        return super.onInterceptTouchEvent(parent, child, event)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        // 记录当前的 RecyclerView
        if (nestedScrollingChildRef == null || nestedScrollingChildRef?.get() != target) {
            nestedScrollingChildRef = WeakReference(target)
        }
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }
}
