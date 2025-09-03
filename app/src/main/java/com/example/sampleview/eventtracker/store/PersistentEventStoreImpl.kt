package com.example.sampleview.eventtracker.store

import com.example.sampleview.eventtracker.model.Event

/**
 * 持久化事件存储实现。
 *
 * 提供将事件持久化、删除以及恢复的接口实现。
 * 当前实现为空实现，实际可扩展为数据库、文件或其他持久化方案。
 */
class PersistentEventStoreImpl : PersistentEventStore {

    /**
     * 将事件持久化保存。
     *
     * @param events 待持久化的事件列表
     */
    override suspend fun persist(events: List<Event>) {
        // 空实现，可替换为具体存储逻辑
    }

    /**
     * 从持久化存储中删除指定事件。
     *
     * @param events 待删除的事件列表
     */
    override suspend fun remove(events: List<Event>) {
        // 空实现，可替换为具体存储逻辑
    }

    /**
     * 从持久化存储中恢复事件。
     *
     * @param limit 最大恢复事件数量
     * @return List<Event> 恢复的事件列表，当前为空
     */
    override suspend fun restore(limit: Int): List<Event> {
        // 空实现，可替换为具体恢复逻辑
        return emptyList()
    }
}
