package com.example.sampleview.trip

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.example.sampleview.R

class TripPointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var topText: String = ""
    private var bottomText: String = ""

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private val topDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bottomDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dashPath = Path()

    private var dotRadius = dpToPx(7f)
    private var dotToTextGap = dpToPx(8f)
    private var paragraphGap = dpToPx(0f)
    private var dashStrokeWidth = dpToPx(1f)
    private var dashGap = dpToPx(2f)

    private var topTextLayout: StaticLayout? = null
    private var bottomTextLayout: StaticLayout? = null

    private var showDashLine = true

    init {
        initAttrs(context, attrs)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.TripPointView) {
            textPaint.textSize = getDimension(R.styleable.TripPointView_textSize, spToPx(14f))
            textPaint.color = getColor(R.styleable.TripPointView_textColor, 0xFF333333.toInt())

            topDotPaint.color = getColor(R.styleable.TripPointView_topDotColor, Color.RED)
            bottomDotPaint.color = getColor(R.styleable.TripPointView_bottomDotColor, Color.RED)
            centerDotPaint.color = getColor(R.styleable.TripPointView_smallDotColor, Color.WHITE)

            dotRadius = getDimension(R.styleable.TripPointView_dotRadius, dotRadius)
            dotToTextGap = getDimension(R.styleable.TripPointView_dotTextGap, dotToTextGap)
            paragraphGap = getDimension(R.styleable.TripPointView_paragraphGap, paragraphGap)

            dashPaint.color = getColor(R.styleable.TripPointView_dashColor, Color.GRAY)
            dashStrokeWidth = getDimension(R.styleable.TripPointView_dashWidth, dashStrokeWidth)
            dashGap = getDimension(R.styleable.TripPointView_dashGap, dashGap)
            showDashLine = getBoolean(R.styleable.TripPointView_showDashLine, true)

            dashPaint.strokeWidth = dashStrokeWidth
            dashPaint.pathEffect = DashPathEffect(floatArrayOf(dashStrokeWidth * 4, dashGap), 0f)
        }
    }

    fun setTexts(top: String, bottom: String) {
        if (top == topText && bottom == bottomText) return
        topText = top
        bottomText = bottom
        requestLayout()
        invalidate()
    }

    fun setTextSizeSp(sp: Float) {
        textPaint.textSize = spToPx(sp)
        requestLayout()
        invalidate()
    }

    fun setTextColor(color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setTopDotColor(color: Int) {
        topDotPaint.color = color
        invalidate()
    }

    fun setBottomDotColor(color: Int) {
        bottomDotPaint.color = color
        invalidate()
    }

    fun setShowDashLine(show: Boolean) {
        if (showDashLine != show) {
            showDashLine = show
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val dotAreaWidth = dotRadius * 2 + dotToTextGap
        val availableTextWidth = maxOf(0, (widthSize - paddingLeft - paddingRight - dotAreaWidth).toInt())

        val topLayout = createStaticLayout(topText, availableTextWidth)
        val bottomLayout = createStaticLayout(bottomText, availableTextWidth)

        topTextLayout = topLayout
        bottomTextLayout = bottomLayout

        val topHeight = topLayout?.let { if (it.lineCount > 0) it.getLineBottom(it.lineCount - 1).toFloat() else 0f } ?: 0f
        val bottomHeight = bottomLayout?.let { if (it.lineCount > 0) it.getLineBottom(it.lineCount - 1).toFloat() else 0f } ?: 0f

        val totalHeight = paddingTop + topHeight + paragraphGap + bottomHeight + paddingBottom

        val desiredWidth = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            val textWidth = maxOf(topLayout?.width ?: 0, bottomLayout?.width ?: 0)
            (paddingLeft + dotAreaWidth + textWidth + paddingRight).toInt()
        }

        setMeasuredDimension(desiredWidth, totalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (topTextLayout == null && bottomTextLayout == null) return

        canvas.withClip(0, 0, width, height) {
            val dotX = paddingLeft + dotRadius + dpToPx(1f)
            val textX = paddingLeft + dotRadius * 2 + dotToTextGap
            val innerDotRadius = dotRadius / 3f

            topTextLayout?.let { layout ->
                val topDotY = paddingTop + dotRadius
                drawDot(this, dotX, topDotY, topDotPaint, innerDotRadius)
                withTranslation(textX, paddingTop.toFloat()) {
                    layout.draw(this)
                }
            }

            bottomTextLayout?.let { layout ->
                val bottomDotY = height - paddingBottom - dotRadius
                val textHeight = if (layout.lineCount > 0) layout.getLineBottom(layout.lineCount - 1).toFloat() else 0f
                val textY = bottomDotY + dotRadius - textHeight
                drawDot(this, dotX, bottomDotY, bottomDotPaint, innerDotRadius)
                withTranslation(textX, textY) {
                    layout.draw(this)
                }
            }

            if (showDashLine && topTextLayout != null && bottomTextLayout != null) {
                val topDotY = paddingTop + dotRadius
                val bottomDotY = height - paddingBottom - dotRadius
                dashPath.reset()
                dashPath.moveTo(dotX, topDotY + dotRadius + dpToPx(1f))
                dashPath.lineTo(dotX, bottomDotY - dotRadius - dpToPx(1f))
                drawPath(dashPath, dashPaint)
            }
        }
    }

    private fun createStaticLayout(text: String, width: Int): StaticLayout? {
        if (text.trim().isEmpty()) return null
        return StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun drawDot(canvas: Canvas, cx: Float, cy: Float, paint: Paint, innerRadius: Float) {
        canvas.drawCircle(cx, cy, dotRadius, paint)
        canvas.drawCircle(cx, cy, innerRadius, centerDotPaint)
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}
