package com.example.sampleview.voc.strategy

import com.example.sampleview.voc.data.model.VocScene
import com.example.sampleview.voc.data.store.VocStore

/**
 * 默认问卷展示策略（DefaultVocStrategy）。
 *
 * 逻辑：如果当前场景 [VocScene] 已存在于历史记录 [VocStore] 中，则返回 true；
 * 否则返回 false。
 */
class DefaultVocStrategy : VocShowStrategy {

    /**
     * 判断问卷是否需要展示。
     *
     * @param scene 当前问卷场景 [VocScene]。
     * @param vocStore 问卷历史记录存储 [VocStore]。
     * @return true 表示场景已存在于历史记录中（已展示过），false 表示未展示。
     */
    override suspend fun shouldShow(scene: VocScene, vocStore: VocStore): Boolean {
        return vocStore.getHistory().contains(scene)
    }
}

