package com.example.sampleview.voc.core

import androidx.fragment.app.FragmentActivity
import com.example.sampleview.voc.data.model.VocResult
import com.example.sampleview.voc.data.model.VocScene
import com.example.sampleview.voc.data.repository.VocRepository
import com.example.sampleview.voc.data.repository.VocRepositoryImpl
import com.example.sampleview.voc.data.store.VocHistoryStore
import com.example.sampleview.voc.data.store.VocStore
import com.example.sampleview.voc.strategy.VocShowStrategy
import com.example.sampleview.voc.ui.VocUI

class VocMediator {

    suspend fun requestVoc(
        activity: FragmentActivity,
        scene: VocScene,
        block: Builder.() -> Unit = {},
    ): VocResult {
        val builder = Builder().apply(block)
        return VocProcessor(builder).start(scene, activity)
    }

    class Builder(
        var vocUI: VocUI? = null,
        var vocShowStrategy: VocShowStrategy? = null,
        var repository: VocRepository = VocRepositoryImpl(),
        var historyStore: VocStore = VocHistoryStore(),
    )
}
