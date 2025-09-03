package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.queue.InMemoryEventQueue
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.RetryingUploader

/**
 * BatchUploadStrategy 是基于队列和批量上传的事件上传策略实现。
 *
 * 特点：
 * 1. 支持将事件加入内存队列和可选持久化存储。
 * 2. 当队列事件数达到批量阈值 [EventTrackerConfig.UploaderConfig.batchSize] 时，触发批量上传。
 * 3. 支持上传失败的重试和事件回滚到队列。
 *
 * @property uploaderConfig 上传器及队列配置
 * @property eventUploader 实际执行上传的 [EventUploader]，默认为带重试机制的 [RetryingUploader]
 * @property store 可选持久化存储，用于保存未上传的事件
 * @property queue 内存事件队列
 */
class BatchUploadStrategy(
    override val uploaderConfig: EventTrackerConfig.UploaderConfig,
    override val eventUploader: EventUploader = RetryingUploader(uploaderConfig),
    override val store: PersistentEventStore? = null,
    override val queue: EventQueue = InMemoryEventQueue(uploaderConfig.queueCapacity),
) : EventUploadStrategy {

    /**
     * 判断当前队列是否已达到批量上传条件。
     *
     * - 当队列中的事件数量大于等于批次阈值 [EventTrackerConfig.UploaderConfig.batchSize] 时返回 true，
     *   表示可以触发批量上传。
     * - 否则返回 false，继续等待更多事件入队。
     *
     * @return true 表示队列已满，可执行批量上传；false 表示队列未满，暂不上传
     */
    override fun shouldUpload(): Boolean {
        return queue.size >= uploaderConfig.batchSize
    }

    /**
     * 处理单个事件。
     *
     * 事件会被加入队列并持久化（如配置了 store）。
     * 当队列大小达到批次阈值 [EventTrackerConfig.UploaderConfig.batchSize] 时，会触发批量上传。
     *
     * @param event 待处理事件
     * @return [EventUploadResult] 上报结果，如果未触发上传返回 [EventUploadResult.Empty]
     */
    override suspend fun handle(event: Event): EventUploadResult {
        queue.offer(event)
        store?.persist(listOf(event))
        return if (shouldUpload()) flushSafe() else EventUploadResult.Queued(listOf(event))
    }

    /**
     * 批量上传队列中缓存的事件。
     *
     * @return [EventUploadResult] 上传结果
     */
    override suspend fun flush(): EventUploadResult = flushSafe()

    /**
     * 安全执行批量上传，将队列中的事件取出上传。
     * 上传失败时会将事件重新入队。
     *
     * @return [EventUploadResult] 上传结果
     */
    private suspend fun flushSafe(): EventUploadResult {
        val batch = queue.pollBatch(uploaderConfig.batchSize)
        if (batch.isEmpty()) return EventUploadResult.Empty
        return runCatching {
            val result = eventUploader.uploadBatch(batch)
            if (result is EventUploadResult.Success) {
                store?.remove(batch)
                EventUploadResult.Success(batch)
            } else {
                batch.forEach { queue.offer(it) }
                EventUploadResult.Failure(batch, Throwable("Upload failed"))
            }
        }.getOrElse { t ->
            batch.forEach { queue.offer(it) }
            EventUploadResult.Failure(batch, t)
        }
    }

    /**
     * 从持久化存储恢复事件到队列中。
     * 通常在应用启动时调用，以保证未上传的事件不会丢失。
     */
    override suspend fun restore() {
        store?.restore()?.forEach { queue.offer(it) }
    }
}
