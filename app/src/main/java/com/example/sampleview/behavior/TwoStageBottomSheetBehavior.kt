package com.example.sampleview.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs

class TwoStageBottomSheetBehavior<V : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BottomSheetBehavior<V>(context, attrs) {

    var middleHeightRatio: Float = 0.5f        // 半展开位置比例（1 - offset/parent）
    var maxExpandedRatio: Float = 0.85f        // 最大展开比例（1 - offset/parent）

    private var parentHeight = 0
    private var halfOffset = 0
    private var maxExpandedOffset = 0

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        val handled = super.onLayoutChild(parent, child, layoutDirection)

        parentHeight = parent.height
        halfOffset = (parentHeight * (1 - middleHeightRatio)).toInt()
        maxExpandedOffset = (parentHeight * (1 - maxExpandedRatio)).toInt()

        // 设置 BottomSheetBehavior 系统参数
        isFitToContents = false
        isHideable = false // 禁止隐藏
        skipCollapsed = true // 禁止折叠
        halfExpandedRatio = middleHeightRatio
        expandedOffset = maxExpandedOffset

        return handled
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int
    ) {
        // 自动吸附到最近的状态
        val top = child.top
        val distToExpanded = abs(top - expandedOffset)
        val distToHalf = abs(top - halfOffset)

        val targetState = if (distToExpanded < distToHalf) STATE_EXPANDED else STATE_HALF_EXPANDED
        setState(targetState)
    }

    override fun setState(state: Int) {
        // 拦截非法状态，强制只允许两段式状态
        when (state) {
            STATE_EXPANDED, STATE_HALF_EXPANDED -> super.setState(state)
            else -> super.setState(STATE_HALF_EXPANDED)
        }
    }
}
