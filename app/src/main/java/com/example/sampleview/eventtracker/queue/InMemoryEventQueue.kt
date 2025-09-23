package com.example.sampleview.eventtracker.queue

import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.model.Event
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存事件队列实现（协程安全，可指定容量）。
 *
 * 使用 [ArrayDeque] + [Mutex] 实现线程安全的事件入队、出队、快照和清空操作。
 * 当队列达到最大容量时，新入队的事件会丢弃最旧的事件以保证容量限制。
 *
 * ### 特性
 * 1. 协程安全：通过 [Mutex] 保证对队列的挂起安全访问。
 * 2. 容量限制：队列最大容量由 [capacity] 控制，避免无限增长。
 * 3. 批量操作：支持批量出队 [pollBatch] 和获取快照 [snapshot]。
 *
 * @property capacity 队列最大容量
 */
class InMemoryEventQueue(private val capacity: Int) : EventQueue {

    /** 内部队列，存储事件，基于 [ArrayDeque] 实现，支持快速头尾插入和删除 */
    private val queue = ArrayDeque<Event>(capacity)

    /** 协程互斥锁，保证对队列的挂起安全访问 */
    private val lock = Mutex()

    /**
     * 获取队列当前大小。
     *
     * - 挂起函数，保证协程安全
     *
     * @return 当前队列中事件数量
     */
    override suspend fun size(): Int = lock.withLock { queue.size }

    /**
     * 判断队列是否为空。
     *
     * - 挂起函数，保证协程安全
     *
     * @return true 如果队列为空，否则 false
     */
    override suspend fun isEmpty(): Boolean = lock.withLock { queue.isEmpty() }

    /**
     * 入队一个事件。
     *
     * - 如果队列已满，会丢弃最旧事件以腾出空间。
     * - 挂起函数，保证协程安全
     *
     * @param event 待入队事件
     */
    override suspend fun offer(event: Event) {
        lock.withLock {
            if (queue.size >= capacity) {
                val removed = queue.removeFirst()
                TrackerLogger.logger.log("InMemoryEventQueue 队列满, 丢弃最旧事件: $removed")
            }
            queue.addLast(event)
        }
    }

    /**
     * 批量入队事件。
     *
     * - 对每个事件依次调用 [offer]，遵循容量限制和丢弃策略
     *
     * @param events 待入队事件列表
     */
    override suspend fun addAll(events: List<Event>) {
        lock.withLock {
            events.forEach { event -> offer(event) }
        }
    }

    /**
     * 批量出队事件。
     *
     * - 最多返回 [max] 个事件
     * - 出队事件会从队列中移除
     * - 队列为空时返回空列表
     *
     * @param max 批量出队的最大数量
     * @return 已出队事件列表
     */
    override suspend fun pollBatch(max: Int): List<Event> {
        return lock.withLock {
            val batchSize = minOf(max, queue.size)
            List(batchSize) { queue.removeFirst() }
        }
    }

    /**
     * 获取队列快照。
     *
     * - 快照为只读列表，不会移除队列中的事件
     * - 可用于观察当前队列状态或进行统计
     *
     * @return 队列当前所有事件的列表
     */
    override suspend fun snapshot(): List<Event> = lock.withLock { queue.toList() }

    /**
     * 清空队列中的所有事件。
     *
     * - 挂起函数，保证协程安全
     */
    override suspend fun clear() = lock.withLock { queue.clear() }
}
