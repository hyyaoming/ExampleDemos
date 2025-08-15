package com.example.sampleview

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

fun Int.res2Color(): Int = ContextCompat.getColor(context, this)

fun Int.res2String(): String = res.getString(this)

fun Int.res2String(vararg args: Any): String = res.getString(this, *args)

fun Int.res2Drawable(): Drawable? = ContextCompat.getDrawable(context, this)

fun Int.res2StringArray(): Array<String> = res.getStringArray(this)

fun Int.res2IntArray(): IntArray = res.getIntArray(this)

fun dp2px(dpValue: Float): Int {
    return (dpValue * res.displayMetrics.density + 0.5f).toInt()
}

fun dp2px_f(dpValue: Float): Float {
    return (dpValue * res.displayMetrics.density + 0.5f)
}

val res: Resources
    get() = context.resources

private val context: Context
    get() = SampleApp.instance
