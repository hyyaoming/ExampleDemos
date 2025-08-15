package com.example.sampleview.reddot.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.R
import com.example.sampleview.dp2px
import com.example.sampleview.reddot.RedDotManager
import com.example.sampleview.reddot.core.RedDotKey
import com.example.sampleview.reddot.core.RedDotValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * 一个用于显示红点或数字徽标的自定义 [View]，通常用于消息未读数、通知提醒等场景。
 *
 * 支持三种显示模式：
 * - [RedDotValue.Empty]：隐藏状态，View 不占布局空间；
 * - [RedDotValue.Dot]：红点提醒；
 * - [RedDotValue.Number]：带数字的徽标，超过 99 显示为“99+”。
 *
 * 可通过 XML 或代码设置样式与状态：
 * - 支持背景颜色、自定义字体大小、文本颜色等；
 * - 自动测量尺寸，支持 `wrap_content`；
 *
 * 示例 XML 使用：
 * ```xml
 * <com.xnhz.libbase.reddot.ui.RedDotView
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     android:layout_margin="8dp"
 *     app:redDotValue="number"        <!-- 红点显示模式：empty / dot / number -->
 *     app:redDotCount="5"             <!-- 当模式为 number 时，显示数字内容 -->
 *     app:redDotBgColor="#FF0000"    <!-- 红点背景颜色，默认红色 -->
 *     app:redDotTextColor="#FFFFFF"  <!-- 数字文本颜色，默认白色 -->
 * />
 * ```
 *
 * 说明：
 * - `app:redDotValue` 可选值：
 *     - 0 或不设置：隐藏红点（Empty）
 *     - 1：显示纯红点（Dot）
 *     - 2：显示数字红点（Number），需配合 `redDotCount` 使用
 * - `app:redDotCount` 表示数字红点的数字大小，超过 99 时自动显示 “99+”
 * - 颜色属性支持使用十六进制颜色字符串或颜色资源引用
 *
 * @constructor 创建红点视图
 * @param context 上下文对象
 * @param attrs 属性集，可用于 XML 配置
 * @param defStyleAttr 默认样式属性
 */

class RedDotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    /**
     * 当前红点值，默认为 [RedDotValue.Empty]
     */
    private var redDotValue: RedDotValue = RedDotValue.Empty

    /**
     * 红点/徽标背景颜色
     */
    private var bgColor: Int = Color.RED

    /**
     * 数字文本颜色
     */
    private var textColor: Int = Color.WHITE

    /**
     * 用于绘制圆角矩形的 RectF 对象
     */
    private val rectF = RectF()

    /**
     * 红点背景画笔
     */
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /**
     * 文本画笔
     */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    /**
     * 当前红点状态监听协程的 Job，用于在需要时取消订阅，避免重复监听和资源泄漏。
     */
    private var currentJob: Job? = null

    /****
     * 持有绑定的 LifecycleOwner 实例，用于协程作用域管理和生命周期感知，
     * 确保订阅能随生命周期自动取消，避免内存泄漏。
     */
    private var lifecycleOwner: LifecycleOwner? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.RedDotView).apply {
            val modeValue = getInt(R.styleable.RedDotView_redDotValue, 0)
            val redDotCount = getInt(R.styleable.RedDotView_redDotCount, 0)
            redDotValue = when (modeValue) {
                1 -> RedDotValue.Dot
                2 -> RedDotValue.Number(redDotCount)
                else -> RedDotValue.Empty
            }
            bgColor = getColor(R.styleable.RedDotView_redDotBgColor, Color.RED)
            textColor = getColor(R.styleable.RedDotView_redDotTextColor, Color.WHITE)
            recycle()
        }

        isClickable = false
        redPaint.color = bgColor
        textPaint.color = textColor
    }

    /**
     * 绑定红点监听器，自动根据传入的 [key] 监听对应红点状态变化。
     *
     * @param key 红点唯一标识，通常对应具体业务功能点。
     * @param lifecycleOwner 生命周期宿主，用于自动管理协程生命周期，避免内存泄漏。
     */
    fun bindRedDotKey(lifecycleOwner: LifecycleOwner, key: RedDotKey) {
        this.currentJob?.cancel()
        this.currentJob = null
        this.lifecycleOwner = lifecycleOwner
        this.currentJob = lifecycleOwner.lifecycleScope.launch {
            RedDotManager.observeRedDotFlow(key)
                .filterNotNull()
                .collect { redDotData ->
                    setRedDotData(redDotData.redDotValue)
                }
        }
    }

    /**
     * 绑定聚合红点监听器，自动根据指定路径 [path] 监听该路径下所有相关红点的聚合状态。
     *
     * @param path 聚合路径，支持批量监听同一前缀下的所有红点数据。
     * @param lifecycleOwner 生命周期宿主，用于自动管理协程生命周期，避免内存泄漏。
     */
    fun bindRedDotPath(lifecycleOwner: LifecycleOwner, path: String) {
        this.currentJob?.cancel()
        this.currentJob = null
        this.lifecycleOwner = lifecycleOwner
        this.currentJob = lifecycleOwner.lifecycleScope.launch {
            RedDotManager.observeAggregateFlow<RedDotValue>(path)
                .filterNotNull()
                .collect { value ->
                    setRedDotData(value)
                }
        }
    }

    /**
     * 设置红点状态对象，控制显示模式与内容。
     *
     * @param redDotValue 红点状态数据
     */
    fun setRedDotData(redDotValue: RedDotValue) {
        this.redDotValue = redDotValue
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = when (redDotValue) {
            RedDotValue.Dot -> dp2px(8f)
            RedDotValue.Empty -> 0
            is RedDotValue.Number -> dp2px(16f)
        }
        val width = resolveSize(desiredSize, widthMeasureSpec)
        val height = resolveSize(desiredSize, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        when (redDotValue) {
            RedDotValue.Dot -> {
                canvas.drawCircle(cx, cy, width / 2f, redPaint)
            }
            is RedDotValue.Number -> {
                val radius = height / 2f
                rectF.set(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawRoundRect(rectF, radius, radius, redPaint)

                textPaint.textSize = height * 0.65f
                val count = (redDotValue as RedDotValue.Number).count
                val countStr = if (count > 99) "99+" else count.toString()
                val yOffset = (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(countStr, cx, cy - yOffset, textPaint)
            }
            else -> Unit
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentJob?.cancel()
    }
}