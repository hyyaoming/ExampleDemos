package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import kotlinx.coroutines.delay

/**
 * RetryingUploader 是一个带重试机制的事件上报器。
 *
 * 它包装了一个基础 [EventUploader]（通过 [EventTrackerConfig.UploaderConfig.uploader] 提供），
 * 当上报单个事件或批量事件失败时，会按照指数退避策略自动重试，
 * 直到达到最大重试次数 [EventTrackerConfig.UploaderConfig.maxRetry]。
 *
 * ## 特性
 * - 支持单条事件和批量事件重试
 * - 使用配置 [uploaderConfig] 提供的上传器及重试策略
 * - 指数退避重试，延迟从 [EventTrackerConfig.UploaderConfig.initialDelay] 开始，最多到 [EventTrackerConfig.UploaderConfig.maxDelay]
 *
 * @property uploaderConfig 上传器及重试策略配置
 */
class RetryingUploader(
    private val uploaderConfig: EventTrackerConfig.UploaderConfig,
) : EventUploader {

    /**
     * 执行带重试机制的 [block]。
     *
     * 如果 [block] 抛出异常，则按照指数退避策略重试，
     * 直到达到 [EventTrackerConfig.UploaderConfig.maxRetry] 次。
     * 超过最大次数仍失败时，会抛出最后一次异常。
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
     * @param event 待上报事件
     * @return [EventUploadResult] 上报结果
     */
    override suspend fun upload(event: Event): EventUploadResult =
        retry { uploaderConfig.uploader.upload(event) }

    /**
     * 批量上报事件，失败时自动重试。
     *
     * @param events 待上报事件列表
     * @return [EventUploadResult] 上报结果
     */
    override suspend fun uploadBatch(events: List<Event>): EventUploadResult =
        retry { uploaderConfig.uploader.uploadBatch(events) }
}
