package com.example.sampleview.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class TwoStageBottomSheetLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var behavior: BottomSheetBehavior<View>
    private var peekHeightPx: Int = dpToPx(200)
    private var callback: Callback? = null

    interface Callback {
        fun onStateChanged(state: Int) {}
        fun onSlide(percent: Float) {} // 0=折叠, 1=完全展开
    }

    fun setCallback(cb: Callback) {
        this.callback = cb
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount != 1) {
            throw IllegalStateException("TwoStageBottomSheetLayout must have exactly one child as content.")
        }

        val sheetView = getChildAt(0)
        behavior = BottomSheetBehavior.from(sheetView).apply {
            isHideable = false
            peekHeight = peekHeightPx
            skipCollapsed = false
            state = BottomSheetBehavior.STATE_COLLAPSED
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    callback?.onStateChanged(newState)
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val percent = slideOffset.coerceIn(0f, 1f)
                    callback?.onSlide(percent)
                }
            })
        }
    }

    fun expand() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapse() {
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun isExpanded(): Boolean = behavior.state == BottomSheetBehavior.STATE_EXPANDED
    fun isCollapsed(): Boolean = behavior.state == BottomSheetBehavior.STATE_COLLAPSED

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
