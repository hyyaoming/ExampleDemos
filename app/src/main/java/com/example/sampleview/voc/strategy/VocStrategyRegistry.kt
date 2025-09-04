package com.example.sampleview.voc.strategy

import com.example.sampleview.voc.data.model.VocScene

class VocStrategyRegistry {
    private val strategyMap: MutableMap<VocScene, VocShowStrategy> = mutableMapOf()

    fun register(scene: VocScene, strategy: VocShowStrategy) {
        strategyMap[scene] = strategy
    }

    fun getStrategy(scene: VocScene): VocShowStrategy {
        return strategyMap[scene] ?: DefaultVocStrategy
    }
}
