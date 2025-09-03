package com.example.sampleview.eventtracker.store

import com.example.sampleview.eventtracker.model.Event

/**
 * 持久化事件存储接口，用于将事件保存在磁盘、数据库或其他长期存储介质中。
 *
 * 实现类需要保证线程安全，且支持挂起操作，以便异步执行 IO 操作。
 */
interface PersistentEventStore {

    /**
     * 批量持久化事件。
     *
     * @param events 待持久化的事件列表
     */
    suspend fun persist(events: List<Event>)

    /**
     * 删除已持久化的事件。
     *
     * @param events 待删除的事件列表
     */
    suspend fun remove(events: List<Event>)

    /**
     * 恢复已持久化的事件。
     *
     * @param limit 最多恢复的事件数量，默认不限制（[Int.MAX_VALUE]）
     * @return 已恢复的事件列表
     */
    suspend fun restore(limit: Int = Int.MAX_VALUE): List<Event>
}
