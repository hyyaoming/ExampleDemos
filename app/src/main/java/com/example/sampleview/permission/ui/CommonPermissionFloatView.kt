package com.example.sampleview.permission.ui

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.example.sampleview.R
import com.example.sampleview.permission.model.PermissionDesc
import com.example.sampleview.permission.util.StatusBarHelper
import com.example.sampleview.permission.util.removeSelfFromParent

/**
 * CommonPermissionFloatView
 *
 * 这是一个自定义的悬浮权限提示视图，继承自 FrameLayout。
 * 该视图可以附加到 Activity 的 DecorView 上，实现权限说明的悬浮显示效果。
 *
 * @param context 上下文
 * @param attrs   XML 属性集，支持布局文件中声明该控件
 */
class CommonPermissionFloatView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    // 初始化时加载布局资源，将 XML 布局文件填充到该 FrameLayout 中
    init {
        LayoutInflater.from(context).inflate(R.layout.permission_common_float_view, this)
    }

    /**
     * 设置悬浮视图中权限描述文本
     *
     * @param permissionDesc 权限描述对象，包含标题和详细说明
     */
    fun bindPermissionDesc(permissionDesc: PermissionDesc?) {
        permissionDesc ?: return
        val title = findViewById<TextView>(R.id.commonFloatTitle)
        val content = findViewById<TextView>(R.id.commonFloatContent)
        title.text = permissionDesc.title
        content.text = permissionDesc.detail
    }

    companion object {

        /**
         * 将悬浮视图添加到 Activity 的 DecorView 中，实现全屏覆盖的悬浮效果。
         * 同时会设置顶部边距为状态栏高度，避免内容被状态栏遮挡。
         *
         * @param activity 目标 Activity，悬浮视图会附加在其 DecorView 上
         * @return 新创建并添加到 DecorView 的 CommonPermissionFloatView 实例，或者 null（附加失败）
         */
        fun attachToDecorView(activity: Activity?): CommonPermissionFloatView? {
            val decorView = activity?.window?.decorView
            if (decorView is ViewGroup) {
                val floatView = CommonPermissionFloatView(activity, null)

                // 设置布局参数，宽高全屏，顶部间距为状态栏高度，避免遮挡
                val layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                layoutParams.topMargin = StatusBarHelper.getStatusBarHeight(activity)

                // 将悬浮视图添加到 decorView
                decorView.addView(floatView, layoutParams)

                // 设置点击事件监听，点击悬浮视图时自动移除，作为兜底策略，方便关闭悬浮视图
                floatView.setOnClickListener { floatView.removeSelfFromParent() }
                return floatView
            }
            // decorView 不存在或类型不对时返回 null
            return null
        }
    }
}
