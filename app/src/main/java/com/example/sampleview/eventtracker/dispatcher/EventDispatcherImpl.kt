package com.example.sampleview.eventtracker.dispatcher

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult
import com.example.sampleview.eventtracker.strategy.EventUploadStrategy
import com.example.sampleview.eventtracker.strategy.UploadStrategyRegistry

/**
 * 默认的事件分发器实现类 [EventDispatcher]。
 *
 * ### 核心职责
 * 1. 根据事件的上传模式选择合适的 [EventUploadStrategy]。
 * 2. 将事件调度到策略进行处理（立即上传或加入批量队列）。
 * 3. 管理事件恢复和批量刷新操作。
 *
 * @property strategyRegistry 上传策略注册表，用于查找适配事件的策略
 */
class EventDispatcherImpl(private val strategyRegistry: UploadStrategyRegistry) : EventDispatcher {

    /**
     * 将单个事件调度到对应的上传策略进行处理。
     *
     * - 查找事件对应的上传策略。
     * - 调用策略的 [EventUploadStrategy.handle] 方法执行事件处理。
     *
     * @param event 待调度的事件
     * @return [EventResult] 表示事件处理结果：
     * - [EventResult.UploadSuccess] 成功上传的事件
     * - [EventResult.UploadFailure] 上传失败事件及异常
     * - [EventResult.Queued] 批量上传策略中已入队但未上传的事件
     */
    override suspend fun dispatch(event: Event): EventResult {
        val strategy = strategyRegistry.findUploadStrategy(event)
        return strategy.handle(event)
    }

    /**
     * 批量刷新所有策略队列，将已缓存的事件进行上报。
     *
     * - 遍历所有注册的策略并调用其 [EventUploadStrategy.flush]。
     * - 捕获刷新过程中可能抛出的异常，将其封装为 [EventResult.UploadFailure]。
     * - 返回合并后的整体结果：
     *   - 若有失败事件，返回最后一个失败结果。
     *   - 若有成功事件且无失败，返回所有成功事件。
     *   - 若无事件处理，返回 [EventResult.Empty]。
     */
    override suspend fun flushAll() {
        strategyRegistry.all().values.map { strategy ->
            strategy.flush()
        }
    }

    /**
     * 从持久化存储恢复未上传事件，并根据策略上传。
     *
     * - 主要用于应用重启或异常恢复场景。
     * - 将持久化存储中的事件重新分发给对应的上传策略：
     *   - 对于批量上传策略，事件会加入队列并等待触发批量上传。
     *   - 对于立即上传策略，事件会立即上传。
     * - 恢复完成后，删除已恢复事件，避免重复上传。
     */
    override suspend fun recoverAndUpload() {
        strategyRegistry.all().entries.forEach { entry ->
            entry.value.recoverAndUpload(entry.key)
        }
    }
}
