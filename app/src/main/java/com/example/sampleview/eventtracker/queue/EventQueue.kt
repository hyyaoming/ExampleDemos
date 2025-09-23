package com.example.sampleview.eventtracker.queue

import com.example.sampleview.eventtracker.model.Event

/**
 * 抽象事件队列接口，用于管理待上传事件。
 *
 * 队列功能包括：
 * 1. 事件入队（可能挂起，等待队列可用空间或执行持久化操作）
 * 2. 批量出队
 * 3. 获取队列快照
 * 4. 清空队列
 * 5. 查询队列状态（是否为空、当前大小）
 *
 * 实现可以是内存队列，也可以是持久化队列（例如数据库或文件存储）。
 */
interface EventQueue {

    /**
     * 将事件入队。
     *
     * - 可能会挂起：
     *   - 内存队列已满时等待可用空间
     *   - 持久化队列执行 IO 操作
     *
     * @param event 待入队事件
     */
    suspend fun offer(event: Event)

    /**
     * 将多个事件批量入队。
     *
     * - 可能会挂起，例如队列容量不足或执行持久化操作。
     *
     * @param events 待入队的事件列表
     */
    suspend fun addAll(events: List<Event>)

    /**
     * 批量出队事件。
     *
     * - 最多返回 [max] 个事件
     * - 出队事件会从队列中移除
     * - 队列为空时返回空列表
     *
     * @param max 最大出队数量
     * @return 已出队事件列表
     */
    suspend fun pollBatch(max: Int): List<Event>

    /**
     * 获取队列快照。
     *
     * - 快照是只读副本，不会移除队列中的事件
     * - 可用于观察当前队列状态或进行统计
     *
     * @return 队列中所有事件的列表
     */
    suspend fun snapshot(): List<Event>

    /**
     * 清空队列。
     *
     * - 移除队列中所有事件
     * - 可能会挂起，例如执行持久化队列的清理操作
     */
    suspend fun clear()

    /**
     * 查询队列是否为空。
     *
     * - 可能会挂起，例如执行 IO 或等待同步状态
     *
     * @return `true` 如果队列为空，否则返回 `false`
     */
    suspend fun isEmpty(): Boolean

    /**
     * 获取当前队列大小。
     *
     * - 可能会挂起，例如执行 IO 或等待同步状态
     *
     * @return 队列中事件数量
     */
    suspend fun size(): Int
}
