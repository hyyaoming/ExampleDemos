package com.example.sampleview.eventtracker.dispatcher

import com.example.sampleview.AppLogger
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.strategy.EventUploadStrategy
import com.example.sampleview.eventtracker.strategy.UploadStrategyRegistry

/**
 * 默认的事件分发器实现类 [EventDispatcher]。
 *
 * 通过 [UploadStrategyRegistry] 管理多种事件上传策略，
 * 并根据事件的配置选择合适的策略进行调度、上传和恢复。
 */
class EventDispatcherImpl(private val strategyRegistry: UploadStrategyRegistry) : EventDispatcher {

    /**
     * 将单个事件调度到对应的上传策略进行处理。
     *
     * @param event 待调度的事件
     * @return [EventUploadResult] 表示事件处理结果，包括：
     * - 成功上传的事件
     * - 失败事件及异常
     * - 被跳过的事件
     * - 空操作（队列未满或无策略）
     */
    override suspend fun dispatch(event: Event): EventUploadResult {
        val strategy = strategyRegistry.findUploadStrategy(event)
            ?: return EventUploadResult.Skipped

        return runCatching {
            strategy.handle(event)
        }.getOrElse { t ->
            EventUploadResult.Failure(listOf(event), t)
        }
    }

    /**
     * 批量刷新所有策略队列，将已缓存的事件进行上报。
     *
     * 遍历所有注册的策略，调用其 [EventUploadStrategy.flush] 方法。
     * 将成功和失败结果合并返回：
     * - 如果有失败，返回最后一个失败结果
     * - 如果有成功事件且无失败，返回所有成功事件
     * - 如果无事件处理，返回 [EventUploadResult.Empty]
     *
     * @return [EventUploadResult] 刷新操作的整体结果
     */
    override suspend fun flushAll(): EventUploadResult {
        val results = strategyRegistry.all().values.map { strategy ->
            runCatching { strategy.flush() }
                .getOrElse { t -> EventUploadResult.Failure(emptyList(), t) }
        }

        val successEvents = results.filterIsInstance<EventUploadResult.Success>()
            .flatMap { it.events }

        val failures = results.filterIsInstance<EventUploadResult.Failure>()

        return when {
            failures.isNotEmpty() -> failures.last()
            successEvents.isNotEmpty() -> EventUploadResult.Success(successEvents)
            else -> EventUploadResult.Empty
        }
    }

    /**
     * 从持久化存储恢复事件到策略队列，用于应用重启或异常恢复场景。
     *
     * 对每个策略调用 [EventUploadStrategy.restore]，
     * 并捕获恢复过程中可能抛出的异常，打印警告日志。
     */
    override suspend fun restoreAll() {
        strategyRegistry.all().values.forEach { strategy ->
            try {
                strategy.restore()
            } catch (t: Throwable) {
                AppLogger.w("EventDispatcher", "Restore failed for ${strategy.javaClass.simpleName}: $t")
            }
        }
    }
}
