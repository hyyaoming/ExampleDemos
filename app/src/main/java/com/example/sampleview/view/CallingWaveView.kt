package com.example.sampleview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.sampleview.R

class CallingWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var initialRadius = 0f
    private var maxRadius = 0f
    private var duration = 3000
    private var speed = 1000
    private var color = ContextCompat.getColor(context, R.color.color_00B14F)
    private var isRunning = false
    private var lastCreateTime: Long = 0
    private val circleList: MutableList<Circle> = ArrayList()
    private val interpolator = LinearInterpolator()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {

        initAttr(context, attrs)
        initPaint()
    }

    fun start() {
        if (!isRunning) {
            isRunning = true
            createCircle.run()
        }
    }

    fun stop() {
        isRunning = false
        circleList.clear()
        invalidate()
    }

    private fun initAttr(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CallingWaveView)
        maxRadius = typedArray.getDimensionPixelSize(
            R.styleable.CallingWaveView_maxRadius,
            getDefaultMaxRadius()
        ).toFloat()
        initialRadius =
            typedArray.getDimensionPixelSize(R.styleable.CallingWaveView_maxRadius, getInitRadius())
                .toFloat()
        color = typedArray.getColor(
            R.styleable.CallingWaveView_color,
            ContextCompat.getColor(context, R.color.color_00B14F)
        )
        duration = typedArray.getInteger(R.styleable.CallingWaveView_duration, 2400)
        speed = typedArray.getInteger(R.styleable.CallingWaveView_speed, 800)
        typedArray.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = (maxRadius * 2).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val iterator = circleList.iterator()
        while (iterator.hasNext()) {
            val circle = iterator.next()
            val radius = circle.currentRadius
            if (System.currentTimeMillis() - circle.mCreateTime < duration) {
                paint.alpha = circle.alpha
                canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, paint)
            } else {
                iterator.remove()
            }
        }
        if (circleList.size > 0) {
            postInvalidateDelayed(10)
        }
    }

    private fun initPaint() {
        paint.style = Paint.Style.FILL
        paint.color = color
    }

    private fun getInitRadius(): Int {
        return (maxRadius * 0.2f).toInt()
    }

    private fun getDefaultMaxRadius(): Int {
        return (context.resources.displayMetrics.widthPixels * 0.24f).toInt()
    }

    private fun newCircle() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCreateTime < speed) {
            return
        }
        val circle = Circle()
        circleList.add(circle)
        invalidate()
        lastCreateTime = currentTime
    }


    private val createCircle: Runnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                newCircle()
                postDelayed(this, speed.toLong())
            }
        }
    }

    private inner class Circle {
        val mCreateTime: Long = System.currentTimeMillis()

        val alpha: Int
            get() {
                val percent: Float = (currentRadius - initialRadius) / (maxRadius - initialRadius)
                return (25.5 - interpolator.getInterpolation(percent) * 25.5).toInt()
            }

        val currentRadius: Float
            get() {
                val percent: Float = (System.currentTimeMillis() - mCreateTime) * 1.0f / duration
                return initialRadius + interpolator.getInterpolation(percent) * (maxRadius - initialRadius)
            }
    }
}