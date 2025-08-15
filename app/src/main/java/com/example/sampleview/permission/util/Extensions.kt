package com.example.sampleview.permission.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

/**
 * 递归查找 Context 所关联的 [Activity]。
 *
 * 如果当前 Context 是 Activity，直接返回。
 * 如果是 [ContextWrapper]，则递归查找其 baseContext。
 * 否则返回 null。
 *
 * @receiver Context 上下文对象
 * @return 关联的 Activity，找不到则返回 null
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 安全地恢复协程挂起函数的执行。
 *
 * 仅当协程仍处于活跃状态时调用 [resume] 恢复协程。
 * 捕获可能出现的异常并打印堆栈，防止程序崩溃。
 *
 * @receiver [CancellableContinuation] 当前协程挂起的续体
 * @param value 用于恢复协程的结果值
 */
fun <T> CancellableContinuation<T>.safeResumeWith(value: T) {
    if (isActive) {
        try {
            resume(value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 安全执行代码块，捕获异常并返回默认值。
 *
 * 用于避免因异常导致程序崩溃，
 * 当执行代码块抛出异常时，打印堆栈并返回指定的默认值。
 *
 * @param block 需要执行的代码块
 * @param default 当异常发生时返回的默认值
 * @return 代码块的返回值，或异常时的默认值
 */
inline fun <T> safeCall(block: () -> T, default: T): T =
    runCatching { block() }.getOrElse {
        it.printStackTrace()
        default
    }


/**
 * 判断指定权限列表是否全部已被授予。
 *
 * @receiver Context 当前上下文，内部会尝试查找对应的 Activity。
 * @param permissions 需要检查的权限列表。
 * @return 若所有权限均已授予则返回 true，否则 false。
 */
fun Context.isPermissionsAllGranted(permissions: List<String>): Boolean {
    val requestList = ArrayList<String>()
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestList.add(permission)
        }
    }
    return requestList.isEmpty()
}

/**
 * 在 Activity 上安全地延迟执行一个 Runnable 任务，避免异常导致崩溃。
 *
 * @param delayTime 延迟时间，默认 200 毫秒
 * @param runnable 延迟执行的任务代码块
 */
fun Activity.runSafeDelayJob(delayTime: Long = 200, runnable: Runnable) {
    try {
        this.window?.decorView?.postDelayed(runnable, delayTime)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 从父容器中移除指定的子 View，安全清理并结束视图过渡动画。
 *
 * @return 是否成功移除（true 表示已移除或本身不在父容器中，false 表示移除失败）
 */
fun View?.removeSelfFromParent(): Boolean {
    this?.let { child ->
        // 如果没有父容器，视为已移除
        if (child.parent == null) {
            return true
        } else {
            // 清除子视图动画，结束视图过渡
            child.clearAnimation()
            (child.parent as ViewGroup?)?.endViewTransition(child)

            // 获取子视图索引，确认子视图在父容器内
            val childIndex = (child.parent as ViewGroup?)?.indexOfChild(child) ?: -1
            if (childIndex >= 0) {
                // 从父容器中移除该视图
                (child.parent as ViewGroup?)?.removeView(child)
                return true
            } else {
                // 子视图不在父容器内，移除失败
                return false
            }
        }
    } ?: run {
        // 传入 null，移除失败
        return false
    }
}
