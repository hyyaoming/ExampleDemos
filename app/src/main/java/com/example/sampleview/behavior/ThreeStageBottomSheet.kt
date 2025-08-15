package com.example.sampleview.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

/**
 * 三段式底部滑动面板控件
 *
 * 支持两种模式：
 * - 两段式（展开、折叠）
 * - 三段式（展开、半展开、折叠）
 *
 * 结合 RecyclerView 嵌套滚动，支持手势滑动、惯性滑动吸附，
 * 以及滑动阻力反馈（超出边界时阻力效果）
 */
class ThreeStageBottomSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        // 状态常量
        const val STATE_EXPANDED = 1        // 完全展开
        const val STATE_HALF_EXPANDED = 2   // 半展开（仅三段模式支持）
        const val STATE_COLLAPSED = 3       // 折叠

        // 模式常量
        const val MODE_TWO_STAGE = 1        // 两段模式
        const val MODE_THREE_STAGE = 2      // 三段模式
    }

    // 当前使用的模式，默认两段式
    private var mode = MODE_TWO_STAGE

    // 当前状态，默认折叠
    private var currentState = STATE_COLLAPSED

    // 记录上一次通知给外部的状态，用于避免重复通知
    private var lastNotifiedState = -1

    // 折叠状态的高度，默认 200dp
    private var collapsedHeight = dp2px(200f)

    // 面板顶部的偏移量（距离父容器顶部的距离）
    private var sheetTop = 0

    // 用于判断滑动触发的最小距离
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // 记录上次触摸的Y坐标
    private var lastY = 0f

    // 是否正在拖拽
    private var dragging = false

    // 最近一次拖拽方向：-1 上滑，1 下滑，0 无
    private var lastDragDirection = 0

    // 外部监听状态变化的回调
    private var onStateChangedListener: ((Int) -> Unit)? = null

    // 外部监听滑动偏移比例（0~1）的回调
    private var onOffsetChangedListener: ((Float) -> Unit)? = null

    // 三个关键偏移值（单位：px）
    private var _minOffset = 0         // 展开时的偏移（最小值）
    private var _maxOffset = 0         // 折叠时的偏移（最大值）
    private var _midOffset = 0         // 半展开时的偏移（中间值，三段模式）

    // 缓存找到的 RecyclerView，用于滑动冲突判断
    private var cachedRecyclerView: RecyclerView? = null

    // 用于实现平滑滚动动画
    private val scroller = OverScroller(context, DecelerateInterpolator())

    // 用于计算滑动速度
    private var velocityTracker: VelocityTracker? = null

    // 系统允许的最大、最小滑动速度
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()

    init {
        // 初始化时等待布局完成再设置偏移
        post {
            updateOffsets()
            sheetTop = _maxOffset      // 默认折叠位置
            currentState = STATE_COLLAPSED
            updateTranslation()
            notifyStateChanged(currentState)
        }
    }

    /**
     * 根据当前模式和控件高度计算三个关键偏移值
     */
    private fun updateOffsets() {
        _minOffset = 0
        _maxOffset = height - collapsedHeight
        _midOffset = if (mode == MODE_THREE_STAGE) {
            height / 2
        } else {
            _maxOffset
        }
    }

    /**
     * 设置模式（两段或三段）
     */
    fun setMode(newMode: Int) {
        if (newMode != MODE_TWO_STAGE && newMode != MODE_THREE_STAGE) return
        mode = newMode
        post {
            updateOffsets()
            // 三段模式转两段时，如果状态是半展开，则切换为折叠
            if (mode == MODE_TWO_STAGE && currentState == STATE_HALF_EXPANDED) {
                setState(STATE_COLLAPSED)
            } else {
                setState(currentState)
            }
        }
    }

    /**
     * 设置折叠状态的高度，单位dp
     */
    fun setCollapsedHeight(dp: Float) {
        collapsedHeight = dp2px(dp)
        post {
            updateOffsets()
            if (currentState == STATE_COLLAPSED) {
                sheetTop = _maxOffset
                updateTranslation()
                notifyStateChanged(currentState)
            }
        }
    }

    /**
     * 设置状态变化监听
     */
    fun setOnStateChangedListener(callback: (Int) -> Unit) {
        onStateChangedListener = callback
    }

    /**
     * 设置偏移比例监听，0 表示展开，1 表示折叠
     */
    fun setOnOffsetChangedListener(callback: (Float) -> Unit) {
        onOffsetChangedListener = callback
    }

    /**
     * 便捷方法：展开
     */
    fun expand() = setState(STATE_EXPANDED)

    /**
     * 便捷方法：半展开，若是两段模式则展开
     */
    fun halfExpand() {
        if (mode == MODE_THREE_STAGE) {
            setState(STATE_HALF_EXPANDED)
        } else {
            setState(STATE_EXPANDED)
        }
    }

    /**
     * 便捷方法：折叠
     */
    fun collapse() = setState(STATE_COLLAPSED)

    /**
     * 获取当前状态
     */
    fun getCurrentState() = currentState

    /**
     * 设置状态，会启动平滑滚动到对应偏移
     */
    fun setState(state: Int) {
        val target = when (state) {
            STATE_EXPANDED -> _minOffset
            STATE_HALF_EXPANDED -> if (mode == MODE_THREE_STAGE) _midOffset else return
            STATE_COLLAPSED -> _maxOffset
            else -> return
        }
        currentState = state
        startScrollTo(target)
        notifyStateChanged(state)
    }

    /**
     * 启动平滑滚动动画
     */
    private fun startScrollTo(targetTop: Int) {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        scroller.startScroll(0, sheetTop, 0, targetTop - sheetTop, 280)
        invalidate()
    }

    /**
     * 计算滚动动画
     */
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            sheetTop = scroller.currY
            updateTranslation()
            updateOffsetRatio()
            postInvalidateOnAnimation()
        }
    }

    /**
     * 更新子 View 的 translationY 实现位移
     */
    private fun updateTranslation() {
        getChildAt(0)?.translationY = sheetTop.toFloat()
    }

    /**
     * 触摸事件拦截逻辑，根据滑动方向和 RecyclerView 状态判断是否拦截
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val y = ev.rawY
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastY = y
                dragging = false
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                cachedRecyclerView = findRecyclerView(getChildAt(0))
                initOrResetVelocityTracker()
                velocityTracker?.addMovement(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = y - lastY
                velocityTracker?.addMovement(ev)
                if (abs(dy) > touchSlop) {
                    val rv = cachedRecyclerView
                    // 向下滑且 RecyclerView 无法向上滚动，或者向上滑且 RecyclerView 无法向下滚动且面板未完全展开时，开始拖拽
                    if ((dy > 0 && (rv?.canScrollVertically(-1) == false)) ||
                        (dy < 0 && (rv?.canScrollVertically(1) == false && sheetTop > _minOffset))
                    ) {
                        dragging = true
                        lastDragDirection = if (dy < 0) -1 else 1
                    }
                }
                if (dragging) lastY = y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                lastDragDirection = 0
                recycleVelocityTracker()
            }
        }
        return dragging
    }

    /**
     * 触摸事件处理，处理拖拽滑动、惯性滑动、阻力效果和自动吸附
     */
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val y = ev.rawY
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastY = y
                dragging = false
                lastDragDirection = 0
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                cachedRecyclerView = findRecyclerView(getChildAt(0))
                initOrResetVelocityTracker()
                velocityTracker?.addMovement(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = y - lastY
                velocityTracker?.addMovement(ev)
                if (!dragging && abs(dy) > touchSlop) {
                    dragging = true
                    lastDragDirection = if (dy < 0) -1 else 1
                }
                if (dragging) {
                    lastY = y

                    val rv = cachedRecyclerView
                    val atTop = sheetTop <= _minOffset
                    val atBottom = sheetTop >= _maxOffset

                    var effectiveDy = dy

                    // 向上滑动且 RecyclerView 可滚动时，面板不滑动
                    if (dy < 0 && rv?.canScrollVertically(1) == true) effectiveDy = 0f

                    // 超出边界时加入阻力效果
                    if ((dy < 0 && atTop) || (dy > 0 && atBottom)) {
                        val overDragDistance =
                            abs(sheetTop + effectiveDy - (if (dy > 0) _maxOffset else _minOffset))
                        val resistanceFactor = resistance(overDragDistance)
                        effectiveDy *= resistanceFactor
                    }

                    // 限制偏移范围
                    val newTop = (sheetTop + effectiveDy).toInt().coerceIn(_minOffset, _maxOffset)
                    if (newTop != sheetTop) {
                        sheetTop = newTop
                        updateTranslation()
                        updateOffsetRatio()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    velocityTracker?.addMovement(ev)
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity)
                    val yVelocity = velocityTracker?.yVelocity ?: 0f
                    flingOrSettle(yVelocity)
                }
                dragging = false
                lastDragDirection = 0
                recycleVelocityTracker()
            }
        }
        return true
    }

    /**
     * 根据惯性速度判断最终吸附位置
     */
    private fun flingOrSettle(velocityY: Float) {
        // 速度不够大则按当前位置自动吸附最近的状态
        if (abs(velocityY) < minFlingVelocity) {
            settleToNearest()
            return
        }

        // 向上滑
        if (velocityY < 0) {
            // 三段模式判断
            if (mode == MODE_THREE_STAGE) {
                if (sheetTop > _midOffset) {
                    // 大于中点，吸附到中点
                    smoothScrollTo(_midOffset)
                    setState(STATE_HALF_EXPANDED)
                } else {
                    // 否则吸附到展开
                    smoothScrollTo(_minOffset)
                    setState(STATE_EXPANDED)
                }
            } else {
                smoothScrollTo(_minOffset)
                setState(STATE_EXPANDED)
            }
        } else { // 向下滑
            if (mode == MODE_THREE_STAGE) {
                if (sheetTop < _midOffset) {
                    smoothScrollTo(_midOffset)
                    setState(STATE_HALF_EXPANDED)
                } else {
                    smoothScrollTo(_maxOffset)
                    setState(STATE_COLLAPSED)
                }
            } else {
                smoothScrollTo(_maxOffset)
                setState(STATE_COLLAPSED)
            }
        }
    }

    /**
     * 根据当前位置吸附到最近状态
     */
    private fun settleToNearest() {
        val distToExpanded = abs(sheetTop - _minOffset)
        val distToHalf = abs(sheetTop - _midOffset)
        val distToCollapsed = abs(sheetTop - _maxOffset)

        val nearestState = when (mode) {
            MODE_THREE_STAGE -> {
                when {
                    distToExpanded <= distToHalf && distToExpanded <= distToCollapsed -> STATE_EXPANDED
                    distToHalf <= distToExpanded && distToHalf <= distToCollapsed -> STATE_HALF_EXPANDED
                    else -> STATE_COLLAPSED
                }
            }

            else -> {
                if (distToExpanded < distToCollapsed) STATE_EXPANDED else STATE_COLLAPSED
            }
        }

        setState(nearestState)
    }

    /**
     * 进行平滑滚动到目标偏移
     */
    private fun smoothScrollTo(target: Int) {
        if (!scroller.isFinished) scroller.abortAnimation()
        scroller.startScroll(0, sheetTop, 0, target - sheetTop, 280)
        postInvalidateOnAnimation()
    }

    /**
     * 更新偏移比例回调，比例范围 [0,1]
     */
    private fun updateOffsetRatio() {
        val ratio = (sheetTop - _minOffset).toFloat() / (_maxOffset - _minOffset)
        onOffsetChangedListener?.invoke(ratio)
    }

    /**
     * 通知状态变化回调，避免重复通知
     */
    private fun notifyStateChanged(state: Int) {
        if (state != lastNotifiedState) {
            lastNotifiedState = state
            onStateChangedListener?.invoke(state)
        }
    }

    /**
     * 递归查找 RecyclerView 用于判断是否能滚动
     */
    private fun findRecyclerView(view: View?): RecyclerView? {
        if (view == null) return null
        if (view is RecyclerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val rv = findRecyclerView(child)
                if (rv != null) return rv
            }
        }
        return null
    }

    /**
     * 初始化或重置速度跟踪器
     */
    private fun initOrResetVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker?.clear()
        }
    }

    /**
     * 释放速度跟踪器
     */
    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    /**
     * 阻力计算函数，距离越大阻力越大，返回比例 [0,1]
     */
    private fun resistance(overDistance: Float): Float {
        // 简单指数阻力
        val maxResistanceDistance = dp2px(100f)
        val ratio = 1 - (overDistance / maxResistanceDistance)
        return max(0.2f, ratio)
    }

    /**
     * dp 转 px
     */
    private fun dp2px(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
