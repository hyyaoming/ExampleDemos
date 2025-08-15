package com.example.sampleview.popupmanager

import android.content.Context
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 基于 [DialogFragment] 的弹窗实现。
 *
 * 挂起展示弹窗，直到 DialogFragment 被关闭或销毁后才返回。
 *
 * ## 特性
 * - 支持优先级队列管理，由 [PopupManager] 调度。
 * - 自动监听 DialogFragment 的关闭事件或生命周期销毁。
 * - show/dismiss 方法必须在主线程执行。
 *
 * @param priority 弹窗优先级，数值越大优先级越高
 * @param tag 弹窗唯一标识，用于队列管理和取消操作
 * @param fragmentManager 用于展示 DialogFragment 的 FragmentManager
 * @param buildDialogFragment 构建 DialogFragment 的函数，传入 context 返回 DialogFragment 实例
 */
class FragmentPopup(
    override val priority: Int,
    override val tag: String,
    private val fragmentManager: FragmentManager,
    private val buildDialogFragment: (Context) -> DialogFragment,
) : PopupController {

    /** 当前显示的 DialogFragment 实例 */
    private var dialogFragment: DialogFragment? = null

    /**
     * 挂起展示弹窗，直到 DialogFragment 被关闭。
     *
     * 必须在主线程执行（UI 线程），通常由 [PopupManager] 调度。
     *
     * @param context 弹窗所在的 Context，通常为 Activity
     */
    override suspend fun show(context: Context) {
        runCatching {
            val fragment = buildDialogFragment(context)
            dialogFragment = fragment
            val transaction = fragmentManager.beginTransaction()
            fragmentManager.findFragmentByTag(tag)?.let { transaction.remove(it) }
            transaction.add(fragment, tag)
            transaction.commitNowAllowingStateLoss()
            waitForDismiss()
        }
    }

    /**
     * 挂起关闭弹窗。
     *
     * 必须在主线程执行（UI 线程），保证 UI 操作安全。
     * 调用后应保证弹窗真正从界面上移除。
     */
    override suspend fun dismiss() {
        runCatching {
            dialogFragment?.dismissAllowingStateLoss()
        }
    }

    /**
     * 挂起直到 DialogFragment 被关闭或销毁。
     *
     * - 监听 Dialog 的 onDismiss 回调
     * - 监听 Fragment 生命周期 onDestroy
     */
    private suspend fun waitForDismiss() = suspendCancellableCoroutine<Unit> { cont ->
        dialogFragment?.dialog?.setOnDismissListener {
            if (cont.isActive) cont.resume(Unit)
        }
        dialogFragment?.lifecycle?.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                if (cont.isActive) cont.resume(Unit)
            }
        })
    }
}


