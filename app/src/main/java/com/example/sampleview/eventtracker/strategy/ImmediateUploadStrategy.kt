package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult
import com.example.sampleview.eventtracker.model.PersistenceMode
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.queue.InMemoryEventQueue
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.RetryingUploader
import kotlinx.coroutines.sync.withLock

/**
 * **ImmediateUploadStrategy** - 即时上传策略。
 *
 * 每个事件在处理时立即尝试上传，失败或网络不可用时会入队等待重试。
 *
 * ### 特性
 * 1. 事件处理立即触发上传；
 * 2. 支持网络不可用或上传失败后的重试机制；
 * 3. 可选内存队列用于暂存未上传事件；
 * 4. 上传结果即时返回。
 *
 * ### 使用场景
 * - 关键事件（如错误日志、重要埋点）需要即时上报；
 * - 不适合高频率事件，避免频繁网络请求。
 *
 * ### 继承关系
 * 继承自 [BatchUploadStrategy]，复用批量上传逻辑，但针对单事件立即上传。
 *
 * @property uploaderConfig 上传器及队列相关配置
 * @property queue 内存队列，存储上传失败或等待重试的事件
 * @property eventUploader 实际上传器，默认使用带重试机制的 [RetryingUploader]
 * @property store 事件持久化存储，用于持久化关键事件
 * @property strategyName 当前策略类的名称
 */
class ImmediateUploadStrategy(
    override val uploaderConfig: EventTrackerConfig.UploaderConfig,
    override val queue: EventQueue = InMemoryEventQueue(uploaderConfig.queueCapacity),
    override val eventUploader: EventUploader = RetryingUploader(uploaderConfig),
    override val store: PersistentEventStore,
    override val strategyName: String = "立即上报策略",
) : BatchUploadStrategy(uploaderConfig, eventUploader, queue, store) {

    /**
     * 处理单个事件。
     *
     * 处理流程：
     * 1. 将事件加入内存队列（保证队列容量限制）；
     * 2. 若事件需要持久化，则写入 [PersistentEventStore]；
     * 3. 判断网络状态：
     *    - 网络不可用：返回 [EventResult.NetworkUnavailable]，事件保留在队列中等待重试；
     *    - 网络可用：调用 [upload] 上传事件，返回上传结果；
     *
     * @param event 待上传事件
     * @return [EventResult] 表示事件处理状态：
     *   - [EventResult.UploadSuccess] 上传成功
     *   - [EventResult.UploadFailure] 上传失败，事件会重新入队等待重试
     *   - [EventResult.NetworkUnavailable] 网络不可用未尝试上传
     *   - [EventResult.Queued] 事件入队等待上传
     */
    override suspend fun handle(event: Event): EventResult {
        TrackerLogger.logger.log("$strategyName 处理事件: $event")
        queue.offer(event)
        TrackerLogger.logger.log("$strategyName 事件已入队, 当前队列大小: ${queue.size()}")
        if (event.persistenceMode == PersistenceMode.ALWAYS_PERSIST) {
            store.persist(listOf(event))
            TrackerLogger.logger.log("$strategyName 事件已持久化: $event")
        }
        return uploadLock.withLock {
            if (!NetworkStateMonitor.isNetworkAvailable) {
                TrackerLogger.logger.log("$strategyName 网络不可用, 事件等待重试: $event")
                return EventResult.NetworkUnavailable(listOf(event))
            } else {
                TrackerLogger.logger.log("$strategyName 网络可用, 准备上传事件: $event")
                val result = upload()
                TrackerLogger.logger.log("$strategyName 上传结果: $result")
                result
            }
        }
    }
}
