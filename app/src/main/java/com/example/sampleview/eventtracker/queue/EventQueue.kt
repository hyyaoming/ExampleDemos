package com.example.sampleview.eventtracker.queue

import com.example.sampleview.eventtracker.model.Event

/**
 * 抽象事件队列，支持事件的入队、批量出队、快照和容量管理。
 *
 * 队列可用于缓存待上报事件，支持内存队列或持久化队列实现。
 * `suspend` 函数表示可能会挂起，例如阻塞等待队列空间或执行 IO 操作。
 */
interface EventQueue {
    /**
     * 将事件入队。
     *
     * 可能会挂起直到队列有空间。
     *
     * @param event 待入队的事件
     */
    suspend fun offer(event: Event)

    /**
     * 批量出队事件。
     *
     * 返回最多 [max] 个事件，如果队列为空则返回空列表。
     * 出队的事件会从队列中移除。
     *
     * @param max 批量出队的最大数量
     * @return 已出队的事件列表
     */
    suspend fun pollBatch(max: Int): List<Event>

    /**
     * 返回队列的快照。
     *
     * 快照是只读副本，不会消费队列中的事件。
     *
     * @return 当前队列中所有事件的列表
     */
    suspend fun snapshot(): List<Event>

    /**
     * 清空队列，移除所有事件。
     */
    suspend fun clear()

    /**
     * 当前队列大小。
     */
    val size: Int
}
