package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult
import kotlinx.coroutines.delay

/**
 * RetryingUploader 是一个带重试机制的事件上报器实现。
 *
 * - 它包装了一个基础 [EventUploader]（由 [EventTrackerConfig.UploaderConfig.uploader] 提供）。
 * - 当单条或批量事件上传失败时，会按照指数退避策略自动重试，直到达到最大重试次数 [EventTrackerConfig.UploaderConfig.maxRetry]。
 *
 * ## 特性
 * 1. 支持单条事件和批量事件重试
 * 2. 使用配置 [uploaderConfig] 提供的上传器和重试策略
 * 3. 指数退避重试：
 *    - 初始延迟为 [EventTrackerConfig.UploaderConfig.initialDelay]
 *    - 每次失败后延迟翻倍，最多不超过 [EventTrackerConfig.UploaderConfig.maxDelay]
 *
 * @property uploaderConfig 上传器及重试策略配置
 */
class RetryingUploader(private val uploaderConfig: EventTrackerConfig.UploaderConfig) : EventUploader {

    /**
     * 执行带重试机制的 [block]。
     *
     * - 如果 [block] 抛出异常，则按照指数退避策略重试。
     * - 超过最大重试次数仍失败时，会抛出最后一次异常。
     *
     * @param block 待执行的上报逻辑
     * @return [block] 返回值
     * @throws Throwable 超过最大重试次数仍然失败时抛出
     */
    private suspend fun <R> retry(block: suspend () -> R): R {
        var currentDelay = uploaderConfig.initialDelay
        repeat(uploaderConfig.maxRetry - 1) {
            try {
                return block()
            } catch (_: Throwable) {
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(uploaderConfig.maxDelay)
            }
        }
        return block()
    }

    /**
     * 上报单个事件，失败时自动重试。
     *
     * - 调用内部 [retry] 方法执行上传逻辑
     * - 返回上传结果 [EventResult]，包含成功或失败信息
     *
     * @param event 待上报事件
     * @return [EventResult] 单条事件上传结果
     */
    override suspend fun upload(event: Event): EventResult =
        retry { uploaderConfig.uploader.upload(event) }

    /**
     * 批量上报事件，失败时自动重试。
     *
     * - 调用内部 [retry] 方法执行批量上传逻辑
     * - 返回上传结果 [EventResult]，包含成功或失败事件
     *
     * @param events 待上报事件列表
     * @return [EventResult] 批量事件上传结果
     */
    override suspend fun uploadBatch(events: List<Event>): EventResult =
        retry { uploaderConfig.uploader.uploadBatch(events) }
}
