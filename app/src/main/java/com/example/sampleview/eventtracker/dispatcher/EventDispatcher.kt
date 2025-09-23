package com.example.sampleview.eventtracker.dispatcher

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.strategy.EventUploadStrategy

/**
 * 事件分发器接口 [EventDispatcher]，负责将事件调度到具体的上传策略进行处理。
 *
 * ### 核心职责
 * 1. 根据事件选择合适的上传策略（如 Immediate / Batch）。
 * 2. 调度事件上传，包括立即上传或加入批量队列。
 * 3. 提供批量刷新接口，将已缓存的事件批量上报。
 * 4. 提供事件恢复接口，将持久化存储的事件重新调度，保证事件不丢失。
 *
 * Dispatcher 是事件上报框架的核心组件之一，用于统一管理事件的上传流程。
 */
interface EventDispatcher {

    /**
     * 调度单个事件到对应的上传策略，并返回处理结果。
     *
     * ### 流程
     * 1. 查找事件匹配的上传策略。
     * 2. 调用策略的 [EventUploadStrategy.handle] 处理事件：
     *    - 对于立即上传策略，事件会立即上传。
     *    - 对于批量上传策略，事件会加入队列，等待批量触发。
     * 3. 如果处理失败，可选择持久化事件。
     *
     * @param event 待调度的事件
     * @return [EventResult] 表示事件处理结果，包括：
     * - [EventResult.UploadSuccess] 成功上传的事件
     * - [EventResult.UploadFailure] 上传失败事件及异常
     * - [EventResult.Queued] 批量上传策略中已入队但未上传的事件
     */
    suspend fun dispatch(event: Event): EventResult

    /**
     * 批量刷新所有策略队列，将已缓存的事件进行上报。
     *
     * ### 使用场景
     * - 手动触发批量上传，例如应用退出前清理队列。
     * - 调试或测试时强制刷新事件。
     *
     * ### 流程
     * 1. 遍历所有注册的上传策略。
     * 2. 调用每个策略的 [EventUploadStrategy.flush]。
     * 3. 捕获刷新过程中可能抛出的异常，并封装为失败结果。
     * 4. 合并所有策略返回的结果：
     *    - 若有失败事件，返回最后一个失败结果。
     *    - 若有成功事件且无失败，返回所有成功事件。
     *    - 若无事件处理，返回 [EventResult.Empty]。
     */
    suspend fun flushAll()

    /**
     * 从持久化存储恢复事件到策略队列中，用于应用重启或异常恢复场景。
     *
     * ### 使用场景
     * - 应用启动时恢复未上报事件。
     * - 异常退出后保证事件不丢失。
     *
     * ### 流程
     * 1. 从 [PersistentEventStore] 恢复未上传事件。
     * 2. 遍历每个事件，重新分发到对应上传策略：
     *    - 对于立即上传策略，事件会立即上传。
     *    - 对于批量上传策略，事件会加入队列，等待批量触发。
     * 3. 恢复完成后，可选择删除已恢复事件，避免重复上传。
     */
    suspend fun recoverAndUpload()
}
