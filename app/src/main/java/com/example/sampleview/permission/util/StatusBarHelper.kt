package com.example.sampleview.permission.util

import android.content.Context

/**
 * 状态栏高度工具类。
 * 支持通过资源和反射获取状态栏高度，兼容魅族特殊尺寸。
 * 若获取失败，使用默认25dp高度兜底。
 */
object StatusBarHelper {

    private const val TAG = "StatusBarHelper"

    /**
     * 大部分状态栏高度默认为25dp
     */
    private const val STATUS_BAR_DEFAULT_HEIGHT_DP = 25

    /**
     * 缓存的状态栏高度（单位：像素）
     */
    private var sStatusBarHeight = -1

    /**
     * 获取状态栏的高度（单位：像素）
     */
    fun getStatusBarHeight(context: Context): Int {
        if (sStatusBarHeight == -1) {
            initStatusBarHeight(context)
        }
        return sStatusBarHeight
    }

    /**
     * 初始化状态栏高度，尝试通过资源、反射获取，最终兜底默认值
     */
    private fun initStatusBarHeight(context: Context) {
        var resId = -1

        try {
            // 魅族手机部分状态栏资源名不同，优先尝试大状态栏高度资源
            if (DeviceUtil.isMeizu()) {
                resId = context.resources.getIdentifier("status_bar_height_large", "dimen", "android")
            }

            // 如果未找到，尝试普通状态栏高度资源
            if (resId <= 0) {
                resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            }

            if (resId > 0) {
                sStatusBarHeight = context.resources.getDimensionPixelSize(resId)
                if (sStatusBarHeight > 0) {
                    return
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        // 资源方式失败，尝试通过反射获取 Android 内部 dimen 类的状态栏高度字段
        try {
            val clazz = Class.forName("com.android.internal.R\$dimen")
            val obj = clazz.getDeclaredConstructor().newInstance()

            val field = if (DeviceUtil.isMeizu()) {
                // 魅族特殊字段名
                try {
                    clazz.getField("status_bar_height_large")
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            } else {
                clazz.getField("status_bar_height")
            }

            if (field != null) {
                val id = when (val value = field.get(obj)) {
                    is Int -> value
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
                if (id > 0) {
                    sStatusBarHeight = context.resources.getDimensionPixelSize(id)
                    if (sStatusBarHeight > 0) {
                        return
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        // 兜底默认值，基于经验的dp转换为px
        sStatusBarHeight = (STATUS_BAR_DEFAULT_HEIGHT_DP * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}




