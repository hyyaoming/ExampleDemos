package com.example.sampleview.voc.core

import androidx.fragment.app.FragmentActivity
import com.example.sampleview.AppLogger
import com.example.sampleview.voc.data.model.VocAction
import com.example.sampleview.voc.data.model.VocResult
import com.example.sampleview.voc.data.model.VocScene
import com.example.sampleview.voc.strategy.VocStrategyRegistry
import com.example.sampleview.voc.ui.DialogVocUI
import com.example.sampleview.voc.ui.VocIntent
import com.example.sampleview.voc.ui.VocUI
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class VocProcessor(private val builder: VocMediator.Builder) {
    private val strategyRegistry = VocStrategyRegistry()

    suspend fun start(scene: VocScene, activity: FragmentActivity): VocResult {
        val history = builder.historyStore.getHistory()
        val question = builder.repository.getQuestions(scene)
        val strategy = builder.vocShowStrategy ?: strategyRegistry.getStrategy(scene)

        if (!strategy.shouldShow(scene, history)) {
            return VocResult.AlreadyShown
        }

        val vocUI = builder.vocUI ?: DialogVocUI()
        vocUI.show(activity, question)

        return vocUI.intents.map { intent ->
            toAction(intent, scene)
        }.mapNotNull { action ->
            handleAction(action, vocUI)
        }.first { result ->
            result is VocResult.Success || result is VocResult.Cancelled
        }
    }

    private fun toAction(intent: VocIntent, vocScene: VocScene): VocAction = when (intent) {
        is VocIntent.Submit -> VocAction.SubmitAnswer(intent.answer, vocScene)
        is VocIntent.Cancel -> VocAction.CancelSurvey(intent.reason)
    }

    private suspend fun handleAction(action: VocAction, ui: VocUI): VocResult? = when (action) {
        is VocAction.SubmitAnswer -> {
            runCatching {
                val success = builder.repository.submitVoc(action.answer)
                if (success) {
                    builder.historyStore.saveHistory(listOf(action.vocScene))
                    ui.dismiss()
                    VocResult.Success(action.answer)
                } else {
                    AppLogger.w("VocProcessor", "submitVoc failed, retry allowed")
                    null
                }
            }.onFailure { e ->
                AppLogger.e("VocProcessor", "submitVoc exception: ${e.message}", e)
            }.getOrNull()
        }
        is VocAction.CancelSurvey -> {
            ui.dismiss()
            VocResult.Cancelled(action.reason)
        }
    }
}
