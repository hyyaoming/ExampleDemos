package com.example.sampleview.voc.data.store

import com.example.sampleview.voc.data.model.VocScene

/**
 * 问卷历史记录存储接口。
 *
 * 提供获取和保存用户问卷展示历史的能力。
 */
interface VocStore {

    /**
     * 获取用户问卷展示历史。
     *
     * @return 已展示的问卷场景列表 [VocScene]。
     */
    suspend fun getHistory(): List<VocScene>

    /**
     * 保存用户问卷展示历史。
     *
     * @param history 需要保存的问卷场景列表 [VocScene]。
     */
    suspend fun saveHistory(history: List<VocScene>)
}

