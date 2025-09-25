package com.example.sampleview.ad

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import kotlin.math.min

class AdCountDownView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
        strokeWidth = 8f
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = "#80FFFFFF".toColorInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val rect = RectF()
    private var radius = 0f
    private var cx = 0f
    private var cy = 0f

    private var currentProgress = 1f
    private var animator: ValueAnimator? = null

    fun startCountDown(duration: Long = 5000L, onFinish: () -> Unit) {
        animator?.cancel()
        currentProgress = 1f
        animator = createAnimator(duration).apply {
            addUpdateListener { animation ->
                currentProgress = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onFinish.invoke()
                }
            })
            start()
        }
    }

    fun cancelCountDown() {
        animator?.cancel()
        animator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = min(w, h) / 2f - ringPaint.strokeWidth
        cx = w / 2f
        cy = h / 2f
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        textPaint.textSize = radius / 1.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(cx, cy, radius, bgPaint)
        canvas.drawArc(rect, -90f, currentProgress * 360f, false, ringPaint)
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("跳过", cx, textY, textPaint)
    }

    private fun createAnimator(duration: Long): ValueAnimator {
        return ValueAnimator.ofFloat(1f, 0f).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
        }
    }
}
