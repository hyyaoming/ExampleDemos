package com.example.sampleview.voc.ui

import androidx.fragment.app.FragmentActivity
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.ui.VocIntent
import kotlinx.coroutines.flow.SharedFlow

interface VocUI {
    val intents: SharedFlow<VocIntent>
    fun show(activity: FragmentActivity, question: Question)
    fun dismiss()
}
