package com.example.sampleview.eventtracker.queue

import androidx.room.concurrent.AtomicBoolean
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.upload.EventUploader
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 内存事件队列实现。
 *
 * 将事件存储在内存队列中，并按指定 [batchSize] 批量上报。
 * 可与 [EventUploader] 配合使用，将事件批量发送到服务器。
 *
 * @property batchSize 批量上报的事件数量阈值，达到该数量会触发 flush
 * @property uploader 用于上报事件的 [EventUploader] 实例
 */
class InMemoryEventQueue(
    private val batchSize: Int = 5,
    private val uploader: EventUploader,
) : EventQueue {

    /** 内部并发队列，用于存储待上报事件 */
    private val queue = ConcurrentLinkedQueue<Event>()

    /** 是否正在执行 flush 操作 */
    private var isFlushing = AtomicBoolean(false)

    /**
     * 将事件加入队列。
     *
     * 如果队列中事件数量达到 [batchSize]，会自动触发 [flush] 上报。
     *
     * @param event 待入队事件
     */
    override suspend fun enqueue(event: Event) {
        queue.add(event)
        persist(event)
        if (queue.size >= batchSize) flush()
    }

    /**
     * 刷新队列，将队列中事件按 [batchSize] 批量上报。
     *
     * 使用 [isFlushing] 保证并发安全，避免重复上报。
     *
     * @return 上报结果 [EventUploadResult]
     */
    override suspend fun flush(): EventUploadResult {
        if (!isFlushing.compareAndSet(false, true)) return EventUploadResult.Enqueued
        try {
            var lastResult: EventUploadResult = EventUploadResult.Failure(RuntimeException("No events to flush"))

            while (queue.isNotEmpty() && queue.size >= batchSize) {
                val events = mutableListOf<Event>()
                repeat(batchSize) {
                    queue.poll()?.let { events.add(it) }
                }
                if (events.isEmpty()) break

                lastResult = uploader.uploadBatch(events)
                if (lastResult is EventUploadResult.Success) removePersisted(events)
            }
            return lastResult
        } finally {
            isFlushing.set(false)
        }
    }

    /**
     * 持久化事件到本地存储。
     *
     * 内存队列实现为空，可根据需要重写实现。
     *
     * @param event 待持久化事件
     */
    override suspend fun persist(event: Event) {
    }

    /**
     * 移除已持久化事件。
     *
     * 内存队列实现为空，可根据需要重写实现。
     *
     * @param event 待移除的事件列表
     */
    override suspend fun removePersisted(event: List<Event>) {
    }

    /**
     * 恢复已持久化事件。
     *
     * 内存队列实现为空，默认返回空列表。
     *
     * @return 已恢复事件列表
     */
    override suspend fun restore(): List<Event> = emptyList()

    /**
     * 获取当前队列快照，用于批量上报或查看队列状态。
     *
     * @return 当前队列中事件列表
     */
    override fun snapshot() = queue.toList()

    /** 当前队列事件数量 */
    override val size: Int get() = queue.size
}
