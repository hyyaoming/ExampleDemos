package com.example.sampleview.html

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.R

class HtmlActivity : AppCompatActivity(R.layout.activity_html) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawHtml = "将在<scope style='color: #70B603;'>5分钟</scope>后自动执行"
        findViewById<TextView>(R.id.tvHtml).text = parseScopeColoredText(rawHtml)
    }
}


fun parseScopeColoredText(input: String): CharSequence {
    if (!input.contains("<scope", ignoreCase = true)) {
        return input
    }
    val spannable = SpannableStringBuilder()
    var lastIndex = 0

    val regex = Regex(
        """<scope\s+style\s*=\s*['"]?\s*color\s*:\s*(#[0-9a-fA-F]{6})\s*;?\s*['"]?\s*>(.*?)</scope>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    for (match in regex.findAll(input)) {
        val start = match.range.first
        val end = match.range.last + 1

        // 加前面的非标签文本
        if (lastIndex < start) {
            spannable.append(input.substring(lastIndex, start))
        }

        val colorStr = match.groups[1]?.value ?: ""
        val text = match.groups[2]?.value ?: ""

        val spanStart = spannable.length
        spannable.append(text)
        val spanEnd = spannable.length

        try {
            val color = Color.parseColor(colorStr)
            spannable.setSpan(
                ForegroundColorSpan(color),
                spanStart,
                spanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } catch (e: Exception) {
            // 颜色非法时不加颜色，但保留中间文字
            Log.w("ScopeParser", "颜色格式错误：$colorStr，将仅保留文本：$text")
            // 不设 span，文字已 append
        }

        lastIndex = end
    }

    // 最后的非标签文本
    if (lastIndex < input.length) {
        spannable.append(input.substring(lastIndex))
    }

    return spannable
}





