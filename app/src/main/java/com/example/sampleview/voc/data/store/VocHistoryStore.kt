package com.example.sampleview.voc.data.store

import com.example.sampleview.voc.data.model.VocScene

/**
 * 问卷历史记录默认实现（VocHistoryStore）。
 *
 * 使用内存列表保存问卷展示历史。
 * 仅在应用运行期间有效，重启后历史会丢，后续可能考虑存入磁盘
 */
class VocHistoryStore : VocStore {

    /** 内部存储问卷展示历史的列表 */
    private val _history = mutableListOf<VocScene>()

    /**
     * 获取用户问卷展示历史。
     *
     * @return 已展示的问卷场景列表 [VocScene]。
     */
    override suspend fun getHistory(): List<VocScene> {
        return _history
    }

    /**
     * 保存用户问卷展示历史。
     *
     * @param history 需要保存的问卷场景列表 [VocScene]。
     */
    override suspend fun saveHistory(history: List<VocScene>) {
        _history.addAll(history)
    }
}
