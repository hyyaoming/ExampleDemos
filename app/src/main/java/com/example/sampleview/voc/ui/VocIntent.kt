package com.example.sampleview.voc.ui

import com.example.sampleview.voc.data.model.Answer

/**
 * 用户问卷操作意图（VocIntent）。
 *
 * 用于表示用户在问卷 UI 中的操作事件。
 */
sealed class VocIntent {

    /**
     * 用户提交问卷答案。
     *
     * @property answer 用户填写的答案 [Answer]。
     */
    data class Submit(val answer: Answer) : VocIntent()

    /**
     * 用户取消问卷。
     *
     * @property reason 用户取消的原因说明。
     */
    data class Cancel(val reason: String) : VocIntent()
}

