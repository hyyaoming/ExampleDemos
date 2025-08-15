package com.example.sampleview.flexbox

import android.content.Context
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.example.sampleview.R

class FontFitTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private val testPaint: TextPaint = TextPaint(paint)
    private var minSize: Float = 0f
    private var maxSize: Float = 0f

    init {
        context.withStyledAttributes(attrs, R.styleable.FontFitTextView) {
            minSize = getDimensionPixelSize(
                R.styleable.FontFitTextView_minTextSize,
                spToPx(20f).toInt()
            ).toFloat()
            maxSize = getDimensionPixelSize(
                R.styleable.FontFitTextView_maxTextSize,
                spToPx(28f).toInt()
            ).toFloat()
        }
    }

    /**
     * Refit the font size so the text fits within the text box width
     */
    private fun refitText(text: String, textWidth: Int) {
        if (textWidth <= 0) return

        val targetWidth = textWidth - paddingLeft - paddingRight
        var hi = maxSize
        var lo = minSize
        val threshold = 0.5f

        testPaint.set(paint)

        testPaint.textSize = maxSize
        if (testPaint.measureText(text) <= targetWidth) {
            lo = maxSize
        } else {
            testPaint.textSize = minSize
            if (testPaint.measureText(text) < targetWidth) {
                while ((hi - lo) > threshold) {
                    val size = (hi + lo) / 2
                    testPaint.textSize = size
                    if (testPaint.measureText(text) >= targetWidth) {
                        hi = size
                    } else {
                        lo = size
                    }
                }
            }
        }

        // 最终设置为较小字体，避免溢出
        setTextSize(TypedValue.COMPLEX_UNIT_PX, lo)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val height = measuredHeight
        refitText(text.toString(), parentWidth)
        setMeasuredDimension(parentWidth, height)
    }

    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, after: Int) {
        refitText(text.toString(), width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw) {
            refitText(text.toString(), w)
        }
    }

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}
