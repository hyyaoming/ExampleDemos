package com.example.sampleview.voc.data.model

sealed interface VocAction {
    data class SubmitAnswer(val answer: Answer, val vocScene: VocScene) : VocAction
    data class CancelSurvey(val reason: String) : VocAction
}