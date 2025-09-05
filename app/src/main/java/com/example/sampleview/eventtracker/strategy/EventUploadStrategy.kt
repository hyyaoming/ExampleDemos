package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.queue.InMemoryEventQueue

/**
 * 事件上传策略接口。
 *
 * 每个策略负责定义事件的上报方式，例如：
 * 1. 即时上传 [ImmediateUploadStrategy] 事件产生后立即上传。
 * 2. 批量上传 [BatchUploadStrategy] 事件入队，达到批次条件时才上传。
 *
 * 通过实现该接口，可以灵活定制事件的处理和上传逻辑。
 */
interface EventUploadStrategy {
    /**
     * 判断策略是否满足条件，可以触发上传。
     *
     * - 对于立即上传策略，通常返回 true；
     * - 对于批量上传策略，可根据队列大小或其他条件判断；
     * - 默认实现返回 true，表示总是可以上传。
     *
     * @return true 表示可以触发上传，false 表示暂不上传
     */
    fun shouldUpload(): Boolean {
        return true
    }

    /**
     * 实际执行上传的 [EventUploader]，可为基础上传器或带重试机制的包装器
     */
    val eventUploader: EventUploader

    /**
     * 上传及队列相关配置，如批量大小、重试次数、延迟等
     */
    val uploaderConfig: EventTrackerConfig.UploaderConfig

    /**
     * 事件队列，用于缓存事件。
     *
     * 可以是内存队列（如 [InMemoryEventQueue]）或者支持持久化的队列。
     * 对于即时上传策略可为空。
     */
    val queue: EventQueue?

    /**
     * 可选的持久化存储，用于在进程重启或异常情况下恢复事件。
     *
     * - 对于支持恢复功能的策略（如批量上传），通常会提供实现
     * - 对于即时上传策略，可为空
     */
    val store: PersistentEventStore?

    /**
     * 处理单个事件。
     *
     * - 策略实现决定事件是立即上传还是入队等待批量上传
     * - 如果队列满或达到上传条件，可能触发批量上传
     *
     * @param event 待处理的事件对象
     * @return [EventUploadResult] 表示事件处理或上传的结果
     */
    suspend fun handle(event: Event): EventUploadResult

    /**
     * 批量上传已缓存的事件。
     *
     * - 如果队列为空，返回 [EventUploadResult.Empty]
     * - 上传成功会移除队列或持久化存储中的事件
     *
     * @return [EventUploadResult] 批量上传结果
     */
    suspend fun flush(): EventUploadResult

    /**
     * 从持久化存储恢复事件到策略队列中（可选）。
     *
     * - 如果没有持久化存储或存储为空，方法可为空实现
     * - 通常在应用启动时调用，以保证事件不会丢失
     */
    suspend fun restore()
}
