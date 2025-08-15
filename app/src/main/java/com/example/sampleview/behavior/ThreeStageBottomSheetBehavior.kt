package com.example.sampleview.behavior

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ThreeStageBottomSheetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        const val STATE_COLLAPSED = 0
        const val STATE_HALF_EXPANDED = 1
        const val STATE_EXPANDED = 2
    }

    // BottomSheet 当前状态
    private var state = STATE_COLLAPSED

    // 回调接口
    interface OnSlideListener {
        /**
         * @param slideOffset 范围 [0f..1f]，0 折叠，1 展开，半展开为 0.5
         */
        fun onSlide(slideOffset: Float)
    }

    interface OnStateChangedListener {
        fun onStateChanged(newState: Int)
    }

    var onSlideListener: OnSlideListener? = null
    var onStateChangedListener: OnStateChangedListener? = null

    // 屏幕高度（父View高度）
    private var parentHeight = 0

    // 三个临界点的纵向位置（距离顶部）
    private var expandedOffset = 0
    private var halfExpandedOffset = 0
    private var collapsedOffset = 0

    // 当前顶部位置
    private var currentTop = 0

    // 动画
    private var animator: ValueAnimator? = null

    // 触摸相关
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // 标记是否正在拖拽
    private var isDragging = false

    // 阻尼距离最大值
    private val maxOverScrollDistance = 100

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (parentHeight == 0) {
            parentHeight = measuredHeight
            val density = resources.displayMetrics.density
            expandedOffset = (50 * density).toInt() // 展开时离顶部50dp
            halfExpandedOffset = parentHeight / 2   // 半展开为屏幕一半
            collapsedOffset = parentHeight - (200 * density).toInt() // 折叠时距离底部200dp

            currentTop = collapsedOffset
        }

        // 让子 View 高度撑满底部面板可用空间
        val child = getChildAt(0)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(parentHeight - expandedOffset, MeasureSpec.EXACTLY)
        child.measure(widthMeasureSpec, childHeightSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val child = getChildAt(0)
        // 将子 View 布局到 currentTop 位置
        val left = paddingLeft
        val top = currentTop
        val right = left + child.measuredWidth
        val bottom = top + child.measuredHeight
        child.layout(left, top, right, bottom)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                lastY = ev.y
                animator?.cancel()
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return false
                val y = ev.getY(pointerIndex)
                val dy = y - lastY
                if (abs(dy) > touchSlop) {
                    isDragging = true
                    lastY = y
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newIndex = if (pointerIndex == 0) 1 else 0
                    activePointerId = ev.getPointerId(newIndex)
                    lastY = ev.getY(newIndex)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                lastY = ev.y
                animator?.cancel()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) return false
                val y = ev.getY(pointerIndex)
                val dy = (y - lastY).toInt()
                lastY = y

                if (!isDragging && abs(dy) > touchSlop) {
                    isDragging = true
                }
                if (isDragging) {
                    moveBy(dy)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newIndex = if (pointerIndex == 0) 1 else 0
                    activePointerId = ev.getPointerId(newIndex)
                    lastY = ev.getY(newIndex)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    settle()
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    /**
     * 处理拖动，带阻尼效果，避免越界
     */
    private fun moveBy(dy: Int) {
        var offset = dy
        if (currentTop <= expandedOffset && dy < 0) {
            // 上拉超过展开，阻力递增
            offset = (dy * 0.5f).toInt()
        }
        if (currentTop >= collapsedOffset && dy > 0) {
            // 下拉超过折叠，阻力递增
            offset = (dy * 0.5f).toInt()
        }
        // 限制最大越界距离
        val newTop = min(max(currentTop + offset, expandedOffset - maxOverScrollDistance), collapsedOffset + maxOverScrollDistance)
        if (newTop != currentTop) {
            currentTop = newTop
            requestLayout()
            dispatchSlide()
        }
    }

    /**
     * 松手后吸附到最近的状态
     */
    private fun settle() {
        val distances = listOf(
            expandedOffset to STATE_EXPANDED, halfExpandedOffset to STATE_HALF_EXPANDED, collapsedOffset to STATE_COLLAPSED
        )
        // 找离 currentTop 最近的状态点
        val (targetTop, targetState) = distances.minByOrNull { abs(currentTop - it.first) } ?: (collapsedOffset to STATE_COLLAPSED)
        animateTo(targetTop, targetState)
    }

    /**
     * 启动平滑动画到目标位置，并通知状态变化
     */
    private fun animateTo(targetTop: Int, targetState: Int) {
        animator?.cancel()
        animator = ValueAnimator.ofInt(currentTop, targetTop).apply {
            duration = 300
            addUpdateListener { anim ->
                currentTop = anim.animatedValue as Int
                requestLayout()
                dispatchSlide()
            }
            doOnEnd {
                setState(targetState)
            }
            start()
        }
    }

    /**
     * 设置状态并通知回调
     */
    private fun setState(newState: Int) {
        if (state != newState) {
            state = newState
            onStateChangedListener?.onStateChanged(state)
        }
    }

    /**
     * 通知滑动偏移，范围0~1
     */
    private fun dispatchSlide() {
        val totalRange = collapsedOffset - expandedOffset
        val offset = (collapsedOffset - currentTop).toFloat() / totalRange
        onSlideListener?.onSlide(offset.coerceIn(0f, 1f))
    }

    /**
     * 暴露状态获取
     */
    fun getState(): Int = state

    /**
     * 外部调用控制展开、半展开、折叠
     */
    fun expand() = animateTo(expandedOffset, STATE_EXPANDED)
    fun halfExpand() = animateTo(halfExpandedOffset, STATE_HALF_EXPANDED)
    fun collapse() = animateTo(collapsedOffset, STATE_COLLAPSED)

    /**
     * 支持简单的 RecyclerView 嵌套滑动拦截示例
     */
    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        return (axes and SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // 当 RecyclerView 向下滑且 BottomSheet 未完全展开时优先处理滑动（即拦截滑动）
        if (dy > 0 && currentTop > expandedOffset) {
            moveBy(-dy)
            consumed[1] = dy
        }
        // 当 RecyclerView 向上滑且 BottomSheet 未折叠时优先处理滑动
        else if (dy < 0 && currentTop < collapsedOffset) {
            moveBy(-dy)
            consumed[1] = dy
        }
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        // 可选实现
    }

    override fun onStopNestedScroll(target: View) {
        settle()
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        // 可选实现
    }
}
