package com.example.sampleview.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class OptimizeBottomSheetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    companion object {
        const val STATE_EXPANDED = 1
        const val STATE_HALF_EXPANDED = 2
        const val STATE_COLLAPSED = 3

        const val MODE_TWO_STAGE = 1
        const val MODE_THREE_STAGE = 2
    }

    private var mode = MODE_TWO_STAGE

    private var currentState = STATE_COLLAPSED
    private var lastNotifiedState = -1
    private var collapsedHeight = dp2px(200f)
    private var sheetTop = 0
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var lastY = 0f
    private var dragging = false
    private var lastDragDirection = 0 // -1 上滑, 1 下滑, 0 无
    private var onStateChangedListener: ((Int) -> Unit)? = null
    private var onOffsetChangedListener: ((Float) -> Unit)? = null
    private var _minOffset = 0
    private var _maxOffset = 0
    private var _midOffset = 0
    private var cachedRecyclerView: RecyclerView? = null
    private val scroller = OverScroller(context, DecelerateInterpolator())

    init {
        post {
            updateOffsets()
            sheetTop = _maxOffset
            currentState = STATE_COLLAPSED
            updateTranslation()
            notifyStateChanged(currentState)
        }
    }

    private fun updateOffsets() {
        _minOffset = 0
        _maxOffset = height - collapsedHeight
        _midOffset = if (mode == MODE_THREE_STAGE) {
            height / 2
        } else {
            // 两折叠模式不支持半展开，midOffset设为collapsed位置
            _maxOffset
        }
    }

    fun setMode(newMode: Int) {
        if (newMode != MODE_TWO_STAGE && newMode != MODE_THREE_STAGE) return
        mode = newMode
        post {
            updateOffsets()
            // 切换模式时，当前状态也要调整
            if (mode == MODE_TWO_STAGE && currentState == STATE_HALF_EXPANDED) {
                setState(STATE_COLLAPSED)
            } else {
                setState(currentState)
            }
        }
    }

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

    fun setOnStateChangedListener(callback: (Int) -> Unit) {
        onStateChangedListener = callback
    }

    fun setOnOffsetChangedListener(callback: (Float) -> Unit) {
        onOffsetChangedListener = callback
    }

    fun expand() = setState(STATE_EXPANDED)

    fun halfExpand() {
        if (mode == MODE_THREE_STAGE) {
            setState(STATE_HALF_EXPANDED)
        } else {
            // 两折叠模式不支持半展开，默认展开
            setState(STATE_EXPANDED)
        }
    }

    fun collapse() = setState(STATE_COLLAPSED)

    fun getCurrentState() = currentState

    fun setState(state: Int) {
        val target = when (state) {
            STATE_EXPANDED -> _minOffset
            STATE_HALF_EXPANDED -> if (mode == MODE_THREE_STAGE) _midOffset else return
            STATE_COLLAPSED -> _maxOffset
            else -> return
        }
        startScrollTo(target)
        currentState = state
        notifyStateChanged(state)
    }

    private fun startScrollTo(targetTop: Int) {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        scroller.startScroll(0, sheetTop, 0, targetTop - sheetTop, 280)
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            sheetTop = scroller.currY
            updateTranslation()
            updateOffsetRatio()
            postInvalidateOnAnimation()
        }
    }

    private fun updateTranslation() {
        getChildAt(0)?.translationY = sheetTop.toFloat()
    }

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
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = y - lastY
                if (abs(dy) > touchSlop) {
                    val rv = cachedRecyclerView
                    if ((dy > 0 && (rv?.canScrollVertically(-1) == false)) || (dy < 0 && (rv?.canScrollVertically(
                            1
                        ) == false && sheetTop > _minOffset))
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
            }
        }
        return dragging
    }

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
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = y - lastY
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

                    if (dy < 0 && rv?.canScrollVertically(1) == true) effectiveDy = 0f

                    if ((dy < 0 && atTop) || (dy > 0 && atBottom)) {
                        val overDragDistance =
                            abs(sheetTop + effectiveDy - (if (dy > 0) _maxOffset else _minOffset))
                        val resistanceFactor = resistance(overDragDistance)
                        effectiveDy *= resistanceFactor
                    }

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
                    // 根据模式和滑动方向决定状态
                    val targetState = when (lastDragDirection) {
                        -1 -> when (currentState) {
                            STATE_COLLAPSED -> if (mode == MODE_THREE_STAGE) STATE_HALF_EXPANDED else STATE_EXPANDED
                            STATE_HALF_EXPANDED -> STATE_EXPANDED
                            else -> STATE_EXPANDED
                        }

                        1 -> when (currentState) {
                            STATE_EXPANDED -> if (mode == MODE_THREE_STAGE) STATE_HALF_EXPANDED else STATE_COLLAPSED
                            STATE_HALF_EXPANDED -> STATE_COLLAPSED
                            else -> STATE_COLLAPSED
                        }

                        else -> currentState
                    }
                    setState(targetState)
                }
                dragging = false
                lastDragDirection = 0
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (childCount > 0) {
            val child = getChildAt(0)
            measureChild(
                child,
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount > 0) {
            val child = getChildAt(0)
            val h = child.measuredHeight
            child.layout(0, 0, width, h)
        }
    }

    private fun updateOffsetRatio() {
        val ratio = 1f - (sheetTop - _minOffset).toFloat() / (_maxOffset - _minOffset)
        onOffsetChangedListener?.invoke(ratio.coerceIn(0f, 1f))
    }

    private fun notifyStateChanged(state: Int) {
        if (state != lastNotifiedState) {
            lastNotifiedState = state
            onStateChangedListener?.invoke(state)
        }
    }

    private fun findRecyclerView(view: View?): RecyclerView? {
        if (view == null) return null
        if (view is RecyclerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findRecyclerView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun resistance(overDragDistance: Float): Float {
        val normalized = (overDragDistance / 300f).coerceAtMost(1f)
        return (1f - normalized * normalized).coerceAtLeast(0.1f)
    }

    private fun dp2px(dp: Float): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

}