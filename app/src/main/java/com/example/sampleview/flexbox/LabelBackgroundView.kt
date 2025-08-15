package com.example.sampleview.flexbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave

class LabelBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val text1 = "888888.88"
    private val text2 = "SDG"
    private var needWrap = false
    private var needWrapLabel1 = false

    // StaticLayout 用于多行文本绘制（标签1）
    private var staticLayoutLabel1: StaticLayout? = null

    private val textPaint1 = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f.sp
    }

    private val textPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 16f.sp
    }

    private val bgPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20303030")
        style = Paint.Style.FILL
    }

    private val bgPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20404040")
        style = Paint.Style.FILL
    }

    private val textBounds1 = Rect()
    private val textBounds2 = Rect()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
        val spacing = 2.dp

        // 获取标签1测量宽度
        textPaint1.getTextBounds(text1, 0, text1.length, textBounds1)
        val text1Width = textBounds1.width()
        val text1Height = -textBounds1.top + textBounds1.bottom

        // 获取标签2测量宽度和高度
        textPaint2.getTextBounds(text2, 0, text2.length, textBounds2)
        val text2Width = textBounds2.width()
        val text2Height = -textBounds2.top + textBounds2.bottom

        // 判断是否换行
        val totalWidth = text1Width + spacing + text2Width
        needWrap = totalWidth > maxWidth

        // 判断标签1是否需要换行（即标签1宽度大于控件宽度）
        needWrapLabel1 = text1Width > maxWidth

        // 如果标签1需要换行，则构造 StaticLayout 计算高度
        staticLayoutLabel1 = if (needWrapLabel1) {
            StaticLayout.Builder.obtain(text1, 0, text1.length, textPaint1, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            null
        }

        // 计算高度
        val height = when {
            needWrapLabel1 -> {
                val label1Height = staticLayoutLabel1?.height ?: text1Height
                label1Height + spacing + text2Height
            }
            needWrap -> {
                text1Height + spacing + text2Height
            }
            else -> {
                maxOf(text1Height, text2Height)
            }
        }

        setMeasuredDimension(maxWidth, height)
    }





    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val spacing = 2.dp

        textPaint2.getTextBounds(text2, 0, text2.length, textBounds2)
        val text2Width = textBounds2.width()
        val text2Height = -textBounds2.top + textBounds2.bottom
        val baseline2 = -textBounds2.top.toFloat()

        if (needWrapLabel1) {
            // 标签1 多行绘制，居中
            val layout = staticLayoutLabel1 ?: return
            canvas.withSave {
                val dx = (width - layout.width) / 2f
                translate(dx, 0f)
                layout.draw(this)
            }

            // 标签2 居中绘制在标签1底部
            val tag2Left = (width - text2Width) / 2f
            val tag2Top = layout.height + spacing
            val tag2Right = tag2Left + text2Width
            val tag2Bottom = tag2Top + text2Height

            canvas.drawRoundRect(tag2Left, tag2Top.toFloat(), tag2Right, tag2Bottom.toFloat(), 6.dp.toFloat(), 6.dp.toFloat(), bgPaint2)
            canvas.drawText(text2, tag2Left, tag2Top + baseline2, textPaint2)

        } else if (needWrap) {
            // 换行，标签1单行居中绘制
            textPaint1.getTextBounds(text1, 0, text1.length, textBounds1)
            val text1Width = textBounds1.width()
            val text1Height = -textBounds1.top + textBounds1.bottom
            val baseline1 = -textBounds1.top.toFloat()

            val tag1Left = (width - text1Width) / 2f
            val tag1Top = 0f
            val tag1Bottom = tag1Top + text1Height

            canvas.drawText(text1, tag1Left, tag1Top + baseline1, textPaint1)

            // 标签2 居中绘制在标签1底部
            val tag2Left = (width - text2Width) / 2f
            val tag2Top = tag1Bottom + spacing

            canvas.drawText(text2, tag2Left, tag2Top + baseline2, textPaint2)

        } else {
            // 同行绘制，整体居中，底部对齐
            textPaint1.getTextBounds(text1, 0, text1.length, textBounds1)
            val text1Width = textBounds1.width()
            val text1Height = -textBounds1.top + textBounds1.bottom
            val baseline1 = -textBounds1.top.toFloat()

            val spacing = 2.dp
            val totalWidth = text1Width + spacing + text2Width
            val startX = (width - totalWidth) / 2f

            val tag1Left = startX
            val tag1Top = 0f
            val tag1Right = tag1Left + text1Width
            val tag1Bottom = tag1Top + text1Height

            val tag2Left = tag1Right + spacing
            val tag2Top = tag1Bottom - text2Height

            canvas.drawText(text1, tag1Left, tag1Top + baseline1, textPaint1)

            canvas.drawText(text2, tag2Left, tag2Top + baseline2, textPaint2)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private val Float.sp: Float
        get() = this * resources.displayMetrics.scaledDensity
}
