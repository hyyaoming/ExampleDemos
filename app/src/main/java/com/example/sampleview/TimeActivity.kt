package com.example.sampleview

import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class TimeActivity : AppCompatActivity(R.layout.activity_time) {

    var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val testTimes = listOf(
            "2025-07-10 00:15:00",
            "2025-07-10 06:30:00",
            "2025-07-10 12:00:00",
            "2025-07-10 12:59:00",
            "2025-07-10 13:00:00",
            "2025-07-10 17:59:00",
            "2025-07-10 18:00:00",
            "2025-07-10 23:59:00",
            "2025-07-09 22:10:00",
            "2025-07-08 10:00:00",
            "2025-07-07 20:00:00"
        )


        val textView = findViewById<TextView>(R.id.tvTime)
        textView.setOnClickListener {
            if (currentIndex < testTimes.count()) {
                val formattedTime = getFormattedTimeMixed24WithNoon(testTimes[currentIndex])
                textView.text = formattedTime
                currentIndex++
            }
        }

        val tvTimeTimestamp = findViewById<TextView>(R.id.tvGetTimestamp)
        tvTimeTimestamp.setOnClickListener {
            val beijingTimeZone = TimeZone.getTimeZone("Asia/Shanghai") ?: return@setOnClickListener
            val calendar: Calendar = Calendar.getInstance(beijingTimeZone)
            val timestamp: Long = calendar.getTimeInMillis()
            tvTimeTimestamp.text = timestamp.toString()
            Log.d("TimeActivity","当前时间戳:$timestamp")
        }

        formatAmountWithFixedFontSize(tvTimeTimestamp,"555558.65","SDG",28,12)
    }


    fun getFormattedTimeMixed24WithNoon(dateStr: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return dateStr
        }

        try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateTime = LocalDateTime.parse(dateStr, inputFormatter)
            val now = LocalDateTime.now()
            val daysBetween = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate()).toInt()

            val hour = dateTime.hour
            val minute = dateTime.minute

            val timePeriod = when (hour) {
                in 0..5 -> "凌晨"
                in 6..11 -> "上午"
                12 -> "中午"
                in 13..17 -> "下午"
                in 18..23 -> "晚上"
                else -> ""
            }
            val timeStr = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

            return when (daysBetween) {
                0 -> "今天 $timePeriod $timeStr"
                1 -> "昨天 $timeStr"
                2 -> "前天 $timeStr"
                else -> dateTime.format(outputFormatter)
            }
        } catch (_: Exception) {
            return dateStr
        }
    }

    fun formatAmountWithFixedFontSize(
        textView: TextView,
        amountText: String,
        unitText: String,
        amountTextSizeSp: Int, // 比如 24
        unitTextSizeSp: Int    // 比如 14
    ) {
        textView.post {
            val fullTextOneLine = "$amountText$unitText"
            val paint = textView.paint
            val availableWidth = textView.width - textView.paddingLeft - textView.paddingRight
            val measuredWidth = paint.measureText(fullTextOneLine)

            val needBreakLine = measuredWidth > availableWidth
            val fullText = if (needBreakLine) "$amountText\n$unitText" else fullTextOneLine
            val spannable = SpannableString(fullText)

            // 设置金额固定字体大小
            spannable.setSpan(
                AbsoluteSizeSpan(amountTextSizeSp, true),
                0,
                amountText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 设置单位固定字体大小
            val unitStart = if (needBreakLine) amountText.length + 1 else amountText.length
            spannable.setSpan(
                AbsoluteSizeSpan(unitTextSizeSp, true),
                unitStart,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 换行时单位居中
            if (needBreakLine) {
                spannable.setSpan(
                    AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                    unitStart,
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            textView.text = spannable
        }
    }



}