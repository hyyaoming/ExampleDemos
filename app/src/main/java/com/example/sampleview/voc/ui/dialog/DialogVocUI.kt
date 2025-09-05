package com.example.sampleview.voc.ui.dialog

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.AppLogger
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.ui.VocIntent
import com.example.sampleview.voc.ui.VocUI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * 问卷对话框 UI 实现（DialogVocUI）。
 *
 * 使用 [VocDialogFragment] 显示问卷问题，并通过 [intents] 收集用户操作（提交或取消）。
 */
class DialogVocUI : VocUI {

    /** 当前展示的 VocDialogFragment 实例 */
    private var fragment: VocDialogFragment? = null

    /** 内部可变 Flow，用于发送用户操作事件 */
    private val _intents = MutableSharedFlow<VocIntent>(extraBufferCapacity = 1)

    /** 外部可观察的用户操作事件流 */
    override val intents: SharedFlow<VocIntent> get() = _intents

    /**
     * 显示问卷 UI。
     *
     * @param activity 当前 Activity，用于显示 DialogFragment。
     * @param question 当前问卷问题 [com.example.sampleview.voc.data.model.Question]。
     */
    override fun show(activity: FragmentActivity, question: Question) {
        val vocFragment = VocDialogFragment.newInstance(question).also { fragment = it }
        vocFragment.onSubmitListener = { answer ->
            emitIntent(activity, VocIntent.Submit(answer))
        }
        vocFragment.onCancelListener = {
            emitIntent(activity, VocIntent.Cancel("User clicked cancel"))
        }
        runCatching {
            if (!activity.isFinishing && !activity.isDestroyed) {
                vocFragment.show(activity.supportFragmentManager, "DialogVocUI")
            } else {
                emitIntent(activity, VocIntent.Cancel("Activity state invalid, emitting Cancel"))
            }
        }.onFailure { e ->
            emitIntent(activity, VocIntent.Cancel("Failed to show VocDialogFragment: ${e.message}"))
        }
    }

    /**
     * 隐藏问卷 UI。
     *
     * 会安全地 dismiss 当前 DialogFragment 并清理引用。
     */
    override fun dismiss() {
        runCatching {
            fragment?.let { frag ->
                if (frag.isAdded) frag.dismissAllowingStateLoss()
            }
        }.onFailure {
            AppLogger.w("DialogVocUI", "dismiss fragment failed: ${it.message}")
        }.also {
            fragment = null
        }
    }

    /**
     * 发送用户操作事件到 intents 流。
     *
     * @param activity 当前 Activity，用于协程启动。
     * @param intent 用户操作 [VocIntent]。
     */
    private fun emitIntent(activity: FragmentActivity, intent: VocIntent) {
        if (!_intents.tryEmit(intent)) {
            activity.lifecycleScope.launch { _intents.emit(intent) }
        }
    }
}