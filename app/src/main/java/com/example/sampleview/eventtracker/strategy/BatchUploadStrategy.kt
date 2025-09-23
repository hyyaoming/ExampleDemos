package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult
import com.example.sampleview.eventtracker.model.PersistenceMode
import com.example.sampleview.eventtracker.model.UploadMode
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.queue.InMemoryEventQueue
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.RetryingUploader

/**
 * **BatchUploadStrategy** - 批量上传策略。
 *
 * 基于内存队列管理事件，达到批量条件时统一上传。
 * 上传失败会重试，并可通过持久化机制保证事件不丢失。
 * 可感知网络状态，避免无效上传。
 *
 * ### 特性
 * 1. 所有事件先写入队列，达到批量条件后统一上传；
 * 2. 上传失败会重试，并将失败事件重新入队；
 * 3. 根据 [UploadMode] 支持持久化恢复；
 * 4. 可感知网络状态，避免无效上传。
 *
 * ### 使用场景
 * - 适合对实时性要求不高，但注重批量高效上传的场景；
 * - 避免高频率网络请求，提升性能和稳定性。
 *
 * @property uploaderConfig 上传配置（批量大小、队列容量等）
 * @property eventUploader 实际上传器（默认使用 [RetryingUploader]，带重试机制）
 * @property queue 内存事件队列（默认 [InMemoryEventQueue]）
 * @property store 事件持久化存储，用于持久化关键事件
 * @property strategyName 当前策略类的名称
 */
open class BatchUploadStrategy(
    override val uploaderConfig: EventTrackerConfig.UploaderConfig,
    override val eventUploader: EventUploader = RetryingUploader(uploaderConfig),
    override val queue: EventQueue = InMemoryEventQueue(uploaderConfig.queueCapacity),
    override val store: PersistentEventStore,
    override val strategyName: String = "批量上报策略",
) : EventUploadStrategy {

    /**
     * 判断队列是否满足批量上传条件。
     *
     * @return `true` 当队列中事件数量 ≥ [EventTrackerConfig.UploaderConfig.batchSize]；
     *         `false` 否则
     */
    override suspend fun shouldUpload(): Boolean {
        return queue.size() >= uploaderConfig.batchSize
    }

    /**
     * 处理单个事件。
     *
     * 逻辑：
     * 1. 将事件加入队列；
     * 2. 对于 [PersistenceMode.ALWAYS_PERSIST] 事件，持久化存储；
     * 3. 如果队列已达到批量上传条件且网络可用，则触发上传；
     * 4. 否则返回排队或网络不可用状态。
     *
     * @param event 待处理事件
     * @return [EventResult] 上传或排队的结果：
     *   - [EventResult.UploadSuccess] 上传成功
     *   - [EventResult.UploadFailure] 上传失败
     *   - [EventResult.NetworkUnavailable] 网络不可用
     *   - [EventResult.Queued] 入队等待上传
     */
    override suspend fun handle(event: Event): EventResult {
        TrackerLogger.logger.log("$strategyName 处理事件: $event")
        queue.offer(event)
        TrackerLogger.logger.log("$strategyName 事件已入队, 当前队列大小: ${queue.size()}")
        if (event.persistenceMode == PersistenceMode.ALWAYS_PERSIST) {
            store.persist(listOf(event))
            TrackerLogger.logger.log("$strategyName 事件已持久化: $event")
        }
        return if (shouldUpload()) {
            if (NetworkStateMonitor.isNetworkAvailable) {
                TrackerLogger.logger.log("$strategyName 队列达到批量上传条件, 网络可用, 准备上传")
                upload()
            } else {
                TrackerLogger.logger.log("$strategyName 队列达到批量上传条件, 但网络不可用")
                EventResult.NetworkUnavailable(listOf(event))
            }
        } else {
            TrackerLogger.logger.log("$strategyName 队列未达到批量上传条件, 事件排队等待: $event")
            EventResult.Queued(listOf(event))
        }
    }

    /**
     * 主动刷新队列中的事件。
     *
     * - 循环检查队列是否满足上传条件；
     * - 若满足且网络可用，则批量上传事件；
     * - 上传失败的事件会重新入队；
     * - 循环直到队列不满足上传条件或网络不可用。
     */
    override suspend fun flush() {
        TrackerLogger.logger.log("$strategyName 开始 flush 队列中的事件")
        var continueUpload = true
        while (continueUpload) {
            if (!NetworkStateMonitor.isNetworkAvailable) {
                TrackerLogger.logger.log("$strategyName 网络不可用, 停止 flush")
                break
            }
            continueUpload = shouldUpload()
            if (continueUpload) {
                val result = upload()
                if (result is EventResult.Empty || result is EventResult.UploadFailure) break
            } else {
                TrackerLogger.logger.log("$strategyName 队列未达到批量上传条件, 停止 flush")
            }
        }
    }

    /**
     * 从持久化存储恢复事件并重新入队。
     *
     * - 用于进程重启或应用崩溃后恢复未上传事件；
     * - 将恢复的事件加入队列并调用 [flush]；
     * - 上传完成后，从持久化存储中移除已成功上传的事件。
     *
     * @param uploadMode 事件的上传模式
     */
    override suspend fun recoverAndUpload(uploadMode: UploadMode) {
        TrackerLogger.logger.log("$strategyName 开始恢复并上传持久化事件, uploadMode=$uploadMode")
        val events = store.restore(uploadMode)
        TrackerLogger.logger.log("$strategyName 恢复到队列中的事件数量: ${events.size}")
        queue.addAll(events)
        flush()
        store.remove(events)
        TrackerLogger.logger.log("$strategyName 恢复事件上传完成, 已从持久化存储中移除成功事件")
    }

    /**
     * 内部批量上传逻辑。
     *
     * - 从队列中取出最多 [EventTrackerConfig.UploaderConfig.batchSize] 个事件；
     * - 调用 [EventUploader.uploadBatch] 上传；
     * - 上传失败：失败事件重新入队，并对 [PersistenceMode.BEST_EFFORT] 事件进行持久化；
     * - 上传成功：从持久化存储移除成功事件；
     * - 队列为空时返回 [EventResult.Empty]。
     *
     * @return 上传结果
     */
    protected suspend fun upload(): EventResult {
        val batch = queue.pollBatch(uploaderConfig.batchSize)
        TrackerLogger.logger.log("$strategyName 准备上传批量事件, 大小=${batch.size}")

        if (batch.isEmpty()) {
            TrackerLogger.logger.log("$strategyName 队列为空, 无需上传")
            EventResult.Empty
        }

        val result = eventUploader.uploadBatch(batch)
        TrackerLogger.logger.log("$strategyName upload 结果: $result")

        if (result is EventResult.UploadFailure) {
            TrackerLogger.logger.log("$strategyName 上传失败, 将事件重新入队: $batch")
            batch.filter { it.persistenceMode == PersistenceMode.BEST_EFFORT }
                .takeIf { it.isNotEmpty() }?.let {
                    store.persist(it)
                    TrackerLogger.logger.log("$strategyName BEST_EFFORT 事件已持久化: $it")
                }
            queue.addAll(batch)
        }
        if (result is EventResult.UploadSuccess) {
            TrackerLogger.logger.log("$strategyName 上传成功, 移除持久化事件: $batch")
            store.remove(batch)
        }

        return result
    }
}
