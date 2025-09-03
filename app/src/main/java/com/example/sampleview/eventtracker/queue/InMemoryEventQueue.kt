package com.example.sampleview.eventtracker.queue

import com.example.sampleview.eventtracker.model.Event
import java.util.concurrent.LinkedBlockingQueue

/**
 * 基于内存的事件队列实现。
 *
 * - 使用 [LinkedBlockingQueue] 保证线程安全
 * - 支持入队、批量出队、快照和清空操作
 * - 队列容量由构造参数 [capacity] 决定，达到容量时入队操作会挂起
 *
 * @property capacity 队列最大容量
 */
class InMemoryEventQueue(private val capacity: Int) : EventQueue {

    /** 内部队列，保证线程安全 */
    private val queue = LinkedBlockingQueue<Event>(capacity)

    /** 当前队列大小 */
    override val size: Int
        get() = queue.size

    /**
     * 入队一个事件。
     *
     * - 如果队列已满，会挂起直到有空间
     *
     * @param event 待入队事件
     */
    override suspend fun offer(event: Event) {
        queue.put(event)
    }

    /**
     * 批量出队事件。
     *
     * - 最多返回 [max] 个事件
     * - 如果队列为空，返回空列表
     *
     * @param max 批量出队的最大数量
     * @return 已出队的事件列表
     */
    override suspend fun pollBatch(max: Int): List<Event> {
        if (max <= 0) return emptyList()
        val batch = ArrayList<Event>(max)
        repeat(max) {
            val e = queue.poll() ?: return@repeat
            batch.add(e)
        }
        return batch
    }

    /**
     * 获取队列快照，不会移除事件。
     *
     * @return 当前队列中所有事件的列表
     */
    override suspend fun snapshot(): List<Event> {
        return queue.toList()
    }

    /**
     * 清空队列，移除所有事件。
     */
    override suspend fun clear() {
        queue.clear()
    }
}
