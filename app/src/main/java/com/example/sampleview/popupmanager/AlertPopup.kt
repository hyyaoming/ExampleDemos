package com.example.sampleview.popupmanager

import android.content.Context
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 基于 [AlertDialog] 的弹窗实现。
 *
 * 挂起展示弹窗，直到用户关闭或调用 [dismiss] 后才返回。
 *
 * ## 特性
 * - 支持优先级队列管理，由 [PopupManager] 调度。
 * - show/dismiss 方法必须在主线程执行。
 *
 * @param priority 弹窗优先级，数值越大优先级越高
 * @param tag 弹窗唯一标识，用于队列管理和取消操作
 * @param buildDialog 构建 [AlertDialog] 的函数，传入 Context 返回实例
 */
class AlertPopup(
    override val priority: Int,
    override val tag: String,
    private val buildDialog: (Context) -> AlertDialog,
) : PopupController {

    /** 当前显示的 AlertDialog 实例 */
    private var dialog: AlertDialog? = null

    /**
     * 挂起展示弹窗。
     *
     * 必须在主线程执行（UI 线程），通常由 [PopupManager] 调度。
     * 调用后会挂起当前协程，直到用户关闭弹窗或调用 [dismiss]。
     *
     * @param context 弹窗所在的 Context，通常为 Activity
     */
    override suspend fun show(context: Context) {
        runCatching {
            dialog = buildDialog(context).apply {
                show()
            }
            waitForDismiss()
        }
    }

    /**
     * 挂起关闭弹窗。
     *
     * 必须在主线程执行（UI 线程），保证 UI 操作安全。
     * 调用后保证弹窗真正从界面移除。
     */
    override suspend fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    /**
     * 挂起直到弹窗关闭。
     *
     * 内部通过 AlertDialog 的 [android.content.DialogInterface.OnDismissListener] 监听关闭事件。
     * 协程取消时也会调用 [dismiss] 确保安全。
     */
    private suspend fun waitForDismiss() = suspendCancellableCoroutine<Unit> { cont ->
        dialog?.setOnDismissListener {
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
