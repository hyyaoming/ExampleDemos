package com.example.sampleview.voc.ui

import androidx.fragment.app.FragmentActivity
import com.example.sampleview.voc.data.model.Question
import kotlinx.coroutines.flow.SharedFlow

/**
 * 问卷 UI 接口。
 *
 * 提供问卷展示和用户操作收集能力。
 * 具体实现可以是对话框、全屏页等。
 */
interface VocUI {

    /**
     * 用户操作事件流。
     *
     * 通过 [VocIntent] 表示用户提交答案或取消问卷。
     */
    val intents: SharedFlow<VocIntent>

    /**
     * 显示问卷 UI。
     *
     * @param activity 当前 Activity，用于显示 UI。
     * @param question 当前问卷问题 [Question]。
     */
    fun show(activity: FragmentActivity, question: Question)

    /**
     * 隐藏问卷 UI。
     */
    fun dismiss()
}

