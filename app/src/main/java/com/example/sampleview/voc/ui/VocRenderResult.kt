package com.example.sampleview.voc.ui

import com.example.sampleview.voc.data.model.Answer

sealed class VocRenderResult {
    data class Submitted(val answer: Answer) : VocRenderResult()
    object Cancelled : VocRenderResult()
}
