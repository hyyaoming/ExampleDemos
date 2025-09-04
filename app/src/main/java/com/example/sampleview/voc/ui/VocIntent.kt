package com.example.sampleview.voc.ui

import com.example.sampleview.voc.data.model.Answer

sealed class VocIntent {
    data class Submit(val answer: Answer) : VocIntent()
    data class Cancel(val reason: String) : VocIntent()
}
