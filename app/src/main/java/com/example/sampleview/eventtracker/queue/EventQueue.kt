package com.example.sampleview.eventtracker.queue

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * 事件队列接口，用于管理待上报的事件。
 *
 * 提供入队、持久化、恢复以及批量操作的方法。
 * 可以实现不同的队列策略，例如内存队列、数据库队列等。
 */
interface EventQueue {

    /**
     * 将事件加入队列。
     *
     * @param event 待入队的事件
     */
    suspend fun enqueue(event: Event)

    /**
     * 刷新队列，将队列中事件批量上报。
     *
     * @return [EventUploadResult] 上报结果
     */
    suspend fun flush(): EventUploadResult

    /**
     * 将事件持久化到存储（可选）。
     *
     * 默认实现为空，可根据需要实现本地缓存。
     *
     * @param event 待持久化事件
     */
    suspend fun persist(event: Event) {}

    /**
     * 移除已持久化的事件。
     *
     * @param event 待移除的事件列表
     */
    suspend fun removePersisted(event: List<Event>)

    /**
     * 恢复已持久化的事件。
     *
     * @return 已恢复的事件列表，默认返回空列表
     */
    suspend fun restore(): List<Event> = emptyList()

    /**
     * 获取队列快照，用于批量上报或查看当前队列状态。
     *
     * @return 当前队列事件列表的快照
     */
    fun snapshot(): List<Event>

    /**
     * 队列中事件数量。
     */
    val size: Int get() = 0
}
