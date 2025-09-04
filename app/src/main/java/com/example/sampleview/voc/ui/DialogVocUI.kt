package com.example.sampleview.voc.ui

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.AppLogger
import com.example.sampleview.voc.data.model.Question
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class DialogVocUI : VocUI {
    private var fragment: VocDialogFragment? = null
    private val _intents = MutableSharedFlow<VocIntent>(extraBufferCapacity = 1)
    override val intents: SharedFlow<VocIntent> get() = _intents

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

    private fun emitIntent(activity: FragmentActivity, intent: VocIntent) {
        if (!_intents.tryEmit(intent)) {
            activity.lifecycleScope.launch { _intents.emit(intent) }
        }
    }
}
