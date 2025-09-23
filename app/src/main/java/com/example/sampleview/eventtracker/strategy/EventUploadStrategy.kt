package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult
import com.example.sampleview.eventtracker.model.UploadMode
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.queue.InMemoryEventQueue
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader

/**
 * **事件上传策略接口**
 *
 * 定义事件的处理与上传规则，是事件上报框架的核心扩展点。
 * 每个实现类负责：
 * 1. 决定事件是立即上传还是入队等待；
 * 2. 管理内存/持久化队列；
 * 3. 调用 [EventUploader] 执行上传。
 *
 * ### 常见实现
 * - [ImmediateUploadStrategy]：事件产生后立即上传；
 * - [BatchUploadStrategy]：事件入队，满足批量条件后再上传。
 *
 * 通过实现该接口，可以灵活定制上传逻辑与队列管理。
 */
interface EventUploadStrategy {
    /**
     * 当前策略的名称
     */
    val strategyName: String

    /**
     * 判断是否满足触发上传的条件。
     *
     * - 即时上传策略：通常始终返回 `true`；
     * - 批量上传策略：根据队列大小、时间间隔等条件判断；
     * - 默认实现：总是返回 `true`。
     *
     * @return `true` 表示可以上传，`false` 表示暂不上传。
     */
    suspend fun shouldUpload(): Boolean {
        return true
    }

    /**
     * 实际执行上传的组件。
     *
     * 可以是基础上传器，也可以是带重试、日志等包装的上传器。
     */
    val eventUploader: EventUploader

    /**
     * 上传及队列相关配置，例如批量大小、重试次数、延迟等。
     */
    val uploaderConfig: EventTrackerConfig.UploaderConfig

    /**
     * 事件队列，用于缓存待上传的事件。
     *
     * - 可为内存队列（如 [InMemoryEventQueue]），也可为持久化队列；
     * - 即时上传策略可能不使用队列，可为 `null`。
     */
    val queue: EventQueue?

    /**
     * 事件持久化存储，用于在进程重启或异常时恢复事件。
     *
     * - 批量上传策略通常提供实现；
     * - 即时上传策略可为 `null`。
     */
    val store: PersistentEventStore

    /**
     * 处理单个事件。
     *
     * 流程：
     * 1. 根据策略决定是立即上传还是入队；
     * 2. 若队列已满足条件，可触发批量上传；
     * 3. 返回事件的处理或上传结果。
     *
     * @param event 待处理事件
     * @return [EventResult] 事件的处理或上传结果：
     * - [EventResult.UploadSuccess] 上传成功
     * - [EventResult.UploadFailure] 上传失败
     * - [EventResult.Queued] 入队等待
     * - [EventResult.Empty] 队列未满或无需上传
     */
    suspend fun handle(event: Event): EventResult

    /**
     * 批量上传已缓存的事件。
     *
     * - 对批量策略：上传队列或存储中的事件；
     * - 上传成功后移除对应事件；
     * - 队列为空时返回 [EventResult.Empty]。
     */
    suspend fun flush()

    /**
     * 从持久化存储恢复事件到队列（可选）。
     *
     * - 若存储为空或策略不支持持久化，此方法可为空实现；
     * - 通常在应用启动时调用，以确保事件不丢失。
     *
     * @param uploadMode 上传模式（如即时、延迟、定时）
     */
    suspend fun recoverAndUpload(uploadMode: UploadMode)
}
