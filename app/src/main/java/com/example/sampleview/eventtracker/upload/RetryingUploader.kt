package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 带重试机制的事件上报器。
 *
 * 该实现包装一个 [base] 上报器，在上报失败时按照指数退避策略进行重试。
 *
 * @param base 实际执行事件上报的 [EventUploader]
 * @param maxRetry 最大重试次数，默认 3 次
 * @param initialDelay 初始重试延迟，默认 1 秒
 * @param maxDelay 最大重试延迟，默认 5 秒
 */
class RetryingUploader(
    private val base: EventUploader,
    private val maxRetry: Int = 3,
    private val initialDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 5.seconds,
) : EventUploader {

    /**
     * 执行带重试机制的 [block]。
     *
     * 如果 [block] 抛出异常，会根据指数退避策略进行重试，直到达到 [maxRetry] 次。
     *
     * @param block 待执行的上报逻辑
     * @return [block] 返回值
     * @throws Throwable 如果超过最大重试次数仍然失败，则抛出最后一次异常
     */
    private suspend fun <T> retry(block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(maxRetry - 1) {
            try {
                return block()
            } catch (_: Throwable) {
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
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
    override suspend fun upload(event: Event): EventUploadResult = retry { base.upload(event) }

    /**
     * 批量上报事件，失败时自动重试。
     *
     * @param events 待上报事件列表
     * @return [EventUploadResult] 上报结果
     */
    override suspend fun uploadBatch(events: List<Event>): EventUploadResult = retry { base.uploadBatch(events) }
}
