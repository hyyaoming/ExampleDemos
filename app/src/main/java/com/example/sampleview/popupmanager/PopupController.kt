package com.example.sampleview.popupmanager

import android.content.Context
import androidx.annotation.UiThread

/**
 * 统一弹窗控制接口，支持管理各种类型的弹窗（Dialog、AlertDialog、View 等）。
 *
 * 通过实现此接口的类可被 [PopupManager] 队列调度展示，支持：
 * - 按优先级顺序展示弹窗
 * - 协程挂起等待弹窗关闭
 * - 协程安全取消操作
 *
 * ## 特性
 * - 优先级控制（[priority]），数值越大优先级越高
 * - 通过 [tag] 唯一标识弹窗，用于取消或队列管理
 * - 弹窗显示与关闭为挂起函数（协程挂起），挂起直到弹窗真正关闭
 * - 弹窗显示和关闭必须在主线程执行 UI 操作
 *
 * ## 使用约束
 * 1. [show] 方法应挂起直到用户关闭弹窗或调用 [dismiss]
 * 2. [dismiss] 方法应挂起直到弹窗完全关闭
 * 3. 支持协程取消时，应在取消时安全关闭弹窗
 *
 * ## 示例
 * ```kotlin
 * class AlertPopup(
 *     override val priority: Int,
 *     override val tag: String,
 *     private val buildDialog: (Context) -> AlertDialog
 * ) : PopupController {
 *     override suspend fun show(context: Context) { ... }
 *     override suspend fun dismiss() { ... }
 * }
 * ```
 */
interface PopupController {

    /** 弹窗优先级，数值越大优先级越高 */
    val priority: Int

    /** 弹窗唯一标识，用于队列管理和取消操作 */
    val tag: String

    /**
     * 挂起展示弹窗。
     *
     * @param context 弹窗所在的 [Context]，通常为 Activity 或 Fragment 的 context
     * @throws kotlinx.coroutines.CancellationException 如果协程被取消，应安全关闭弹窗
     * @throws Exception 其他异常会被 [PopupManager] 捕获，用于日志或错误处理
     */
    @UiThread
    suspend fun show(context: Context)

    /**
     * 挂起关闭弹窗。
     *
     * 可用于手动取消或队列管理时关闭当前弹窗。
     * 调用后应保证弹窗真正从界面上移除。
     *
     * @throws Exception 如果关闭弹窗过程中出现异常，可被上层捕获
     */
    @UiThread
    suspend fun dismiss()
}
