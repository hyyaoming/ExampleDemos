package com.xnhz.libbase.dialog

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.example.sampleview.dp2px
import com.example.sampleview.res2Color

class LoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var size = dp2px(32f)
    private var animateValue = 0
    private var animator: ValueAnimator? = null
    private var paint = Paint()
    private val lineCount: Int = 12
    private val degreePerLine: Int = 360 / lineCount

    init {
        paint.color = android.R.color.white.res2Color()
        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setSize(size: Int) {
        this.size = size
        requestLayout()
    }

    fun start() {
        if (animator == null) {
            animator = ValueAnimator.ofInt(0, lineCount - 1).apply {
                addUpdateListener(mUpdateListener)
                duration = 600
                repeatMode = ValueAnimator.RESTART
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        } else if (animator?.isRunning == false) {
            animator?.start()
        }
    }

    fun stop() {
        animator?.let {
            if (it.isRunning) {
                animator?.removeUpdateListener(mUpdateListener)
                it.removeAllUpdateListeners()
                it.cancel()
            }
        }
        animator = null
        animateValue = 0
        invalidate()
    }

    private val mUpdateListener = AnimatorUpdateListener { animation ->
        animateValue = animation.animatedValue as Int
        invalidate()
    }

    private fun drawLoading(canvas: Canvas, rotateDegrees: Int) {
        val width = size / 12
        val height = size / 6
        paint.strokeWidth = width.toFloat()
        canvas.rotate(rotateDegrees.toFloat(), (size / 2).toFloat(), (size / 2).toFloat())
        canvas.translate((size / 2).toFloat(), (size / 2).toFloat())
        for (i in 0 until lineCount) {
            canvas.rotate(degreePerLine.toFloat())
            paint.alpha = (255f * (i + 1) / lineCount).toInt()
            canvas.translate(0f, (-size / 2 + width / 2).toFloat())
            canvas.drawLine(0f, 0f, 0f, height.toFloat(), paint)
            canvas.translate(0f, (size / 2 - width / 2).toFloat())
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count =
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null, Canvas.ALL_SAVE_FLAG)
        drawLoading(canvas, animateValue * degreePerLine)
        canvas.restoreToCount(count)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            start()
        } else {
            stop()
        }
    }
}