package com.example.sampleview.voc.strategy

import com.example.sampleview.voc.data.model.VocScene
import com.example.sampleview.voc.data.store.VocStore

/**
 * 问卷展示策略接口（协程版）。
 *
 * 根据问卷场景和历史记录决定当前问卷是否需要展示。
 * 方法为 suspend，可在协程中进行异步判断（例如查询数据库或网络）。
 */
interface VocShowStrategy {

    /**
     * 判断问卷是否需要展示。
     *
     * @param scene 当前问卷场景 [VocScene]。
     * @param vocStore 问卷历史记录存储 [VocStore]，用于查询已展示的问卷。
     * @return true 表示需要展示，false 表示不展示。
     */
    suspend fun shouldShow(scene: VocScene, vocStore: VocStore): Boolean
}

