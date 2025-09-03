package com.example.sampleview.eventtracker.dispatcher

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * 事件分发器接口，负责将事件调度到具体的上传策略进行处理。
 *
 * Dispatcher 是事件上报框架的核心组件之一，用于：
 * 1. 根据事件选择合适的上传策略
 * 2. 调度事件上传
 * 3. 批量刷新策略队列
 * 4. 恢复持久化存储的事件到策略队列
 */
interface EventDispatcher {

    /**
     * 调度单个事件到对应的上传策略，并返回处理结果。
     *
     * @param event 待调度的事件
     * @return [EventUploadResult] 表示事件处理结果，包括成功、失败、跳过或空操作
     */
    suspend fun dispatch(event: Event): EventUploadResult

    /**
     * 批量刷新所有策略队列，将已缓存的事件进行上报。
     *
     * @return [EventUploadResult] 表示刷新操作的整体结果，可能包含成功和失败事件
     */
    suspend fun flushAll(): EventUploadResult

    /**
     * 从持久化存储恢复事件到策略队列中，用于应用重启或异常恢复场景。
     */
    suspend fun restoreAll()
}
