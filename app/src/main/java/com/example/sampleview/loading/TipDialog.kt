package com.example.sampleview.loading

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import com.example.sampleview.R
import com.example.sampleview.dp2px
import com.example.sampleview.dp2px_f
import com.example.sampleview.res2Color
import com.xnhz.libbase.dialog.LoadingView

class TipDialog(context: Context, style: Int) : AppCompatDialog(context, style) {
    init {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(false)
    }

    override fun dismiss() {
        var context: Context? = context
        if (context is ContextWrapper) {
            context = context.baseContext
        }
        if (context is Activity) {
            val activity = context
            if (activity.isDestroyed || activity.isFinishing) {
                return
            }
            super.dismiss()
        } else {
            try {
                super.dismiss()
            } catch (ignore: Throwable) {
            }
        }
    }

    class Builder(private val context: Context) {
        private var tipWOrd: CharSequence = ""
        private var dialogWidth = dp2px(80f)
        private var dialogHeight = dp2px(80f)
        private var tipTopMargin = dp2px(8f)
        private var tipTextColor = android.R.color.white.res2Color()
        private var tipTextSize = 14f
        private var dialogBackground = GradientDrawable().apply {
            cornerRadius = dp2px_f(15f)
            setColor(Color.parseColor("#C0000000"))
        }

        fun setTipTopMargin(topMargin: Int) = apply {
            this.tipTopMargin = topMargin
        }

        fun setDialogWidth(width: Int) = apply {
            this.dialogWidth = width
        }

        fun setDialogHeight(height: Int) = apply {
            this.dialogHeight = height
        }

        fun setTipWord(tipWord: CharSequence) = apply {
            this.tipWOrd = tipWord
        }

        @JvmOverloads
        fun create(cancelable: Boolean = true, style: Int = R.style.TipDialog): TipDialog {
            val dialog = TipDialog(context, style)
            dialog.setCancelable(cancelable)
            val dialogView = LinearLayout(context)
            dialogView.layoutParams = ViewGroup.LayoutParams(dialogWidth, dialogHeight)
            dialogView.orientation = VERTICAL
            dialogView.gravity = Gravity.CENTER
            dialogView.background = dialogBackground
            val loadingView = LoadingView(context)
            val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT
            val loadingParams = LinearLayout.LayoutParams(wrapContent, wrapContent)
            dialogView.addView(loadingView, loadingParams)
            if (tipWOrd.isNotEmpty()) {
                val tipView = TextView(context)
                tipView.ellipsize = TextUtils.TruncateAt.END
                tipView.gravity = Gravity.CENTER
                tipView.textSize = tipTextSize
                tipView.text = tipWOrd
                tipView.setTextColor(tipTextColor)
                val tipParams = LinearLayout.LayoutParams(wrapContent, wrapContent)
                tipParams.topMargin = tipTopMargin
                dialogView.addView(tipView, tipParams)
            }
            dialog.setContentView(dialogView)
            return dialog
        }
    }
}
