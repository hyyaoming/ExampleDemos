package com.example.sampleview.voc.strategy

import com.example.sampleview.voc.data.model.VocScene

/**
 * 问卷展示策略注册表（VocStrategyRegistry）。
 *
 * 用于为不同问卷场景 [VocScene] 注册对应的展示策略 [VocShowStrategy]，
 * 并在需要时获取策略。
 */
class VocStrategyRegistry {

    /** 存储场景与策略的映射关系 */
    private val strategyMap: MutableMap<VocScene, VocShowStrategy> = mutableMapOf()

    /**
     * 注册指定场景的问卷展示策略。
     *
     * @param scene 问卷场景 [VocScene]。
     * @param strategy 对应的展示策略 [VocShowStrategy]。
     */
    fun register(scene: VocScene, strategy: VocShowStrategy) {
        strategyMap[scene] = strategy
    }

    /**
     * 获取指定场景的问卷展示策略。
     *
     * 如果未注册任何策略，返回默认策略 [DefaultVocStrategy]。
     *
     * @param scene 问卷场景 [VocScene]。
     * @return 对应的展示策略 [VocShowStrategy]。
     */
    fun getStrategy(scene: VocScene): VocShowStrategy {
        return strategyMap[scene] ?: DefaultVocStrategy()
    }
}

