package com.example.sampleview.flexbox

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import kotlin.math.max

class AmountUnitView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val amountView = TextView(context).apply {
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setBackgroundColor(Color.parseColor("#33FF0000")) // 半透明红色背景
    }

    private val unitView = TextView(context).apply {
        setTextColor(Color.GRAY)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setBackgroundColor(Color.parseColor("#330000FF")) // 半透明蓝色背景
    }

    private var spacingPx = dpToPx(2f)

    init {
        addView(amountView)
        addView(unitView)
    }

    fun setAmountText(text: String) {
        amountView.text = text
        requestLayout()
        invalidate()
    }

    fun setUnitText(text: String) {
        unitView.text = text
        requestLayout()
        invalidate()
    }

    fun setTextSize(amountSp: Float, unitSp: Float) {
        amountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, amountSp)
        unitView.setTextSize(TypedValue.COMPLEX_UNIT_SP, unitSp)
        requestLayout()
        invalidate()
    }

    fun setSpacingDp(dp: Float) {
        spacingPx = dpToPx(dp)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight

        // 测量两个TextView，宽度不限制高度包裹内容
        amountView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        unitView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val combinedWidth = amountView.measuredWidth + spacingPx + unitView.measuredWidth

        val measuredWidth = when {
            combinedWidth <= maxWidth -> combinedWidth
            else -> max(amountView.measuredWidth, unitView.measuredWidth)
        }

        val measuredHeight = when {
            combinedWidth <= maxWidth -> max(amountView.measuredHeight, unitView.measuredHeight)
            else -> amountView.measuredHeight + unitView.measuredHeight + spacingPx
        }

        setMeasuredDimension(
            resolveSize(measuredWidth + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(measuredHeight + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val startX = paddingLeft
        val startY = paddingTop

        val maxWidth = r - l - paddingLeft - paddingRight

        val combinedWidth = amountView.measuredWidth + spacingPx + unitView.measuredWidth

        if (combinedWidth <= maxWidth) {
            // 一行，底部对齐
            val maxHeight = max(amountView.measuredHeight, unitView.measuredHeight)
            val amountTop = startY + (maxHeight - amountView.measuredHeight)
            val unitTop = startY + (maxHeight - unitView.measuredHeight)

            amountView.layout(
                startX,
                amountTop,
                startX + amountView.measuredWidth,
                amountTop + amountView.measuredHeight
            )
            unitView.layout(
                startX + amountView.measuredWidth + spacingPx,
                unitTop,
                startX + amountView.measuredWidth + spacingPx + unitView.measuredWidth,
                unitTop + unitView.measuredHeight
            )
        } else {
            // 换行，纵向排列，顶部对齐
            amountView.layout(
                startX,
                startY,
                startX + amountView.measuredWidth,
                startY + amountView.measuredHeight
            )
            unitView.layout(
                startX,
                startY + amountView.measuredHeight + spacingPx,
                startX + unitView.measuredWidth,
                startY + amountView.measuredHeight + spacingPx + unitView.measuredHeight
            )
        }
    }

    private fun dpToPx(dp: Float): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}
