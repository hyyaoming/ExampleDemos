package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult

/**
 * 事件上报器接口。
 *
 * 定义事件的单条和批量上报行为。
 * EventUploader 是事件上报框架的核心组件之一，负责将事件发送到目标端点。
 *
 * ### 使用场景
 * - 上报事件到远程服务器（HTTP/HTTPS 请求）
 * - 写入本地日志文件或数据库进行持久化
 * - 自定义处理逻辑，例如发送到第三方 SDK 或消息队列
 */
interface EventUploader {

    /**
     * 上报单个事件。
     *
     * @param event 待上报的事件对象
     * @return [EventResult] 表示事件上报的结果：
     * - [EventResult.UploadSuccess] 上报成功
     * - [EventResult.UploadFailure] 上报失败
     * - [EventResult.Empty] 如果事件未处理或条件未满足
     */
    suspend fun upload(event: Event): EventResult

    /**
     * 批量上报事件。
     *
     * 适用于批量上传策略：
     * - 可以将内存队列或持久化队列中的事件一次性发送
     * - 可提升上传效率，减少网络请求次数
     *
     * @param events 待上报的事件列表
     * @return [EventResult] 表示批量上传的结果：
     * - [EventResult.UploadSuccess] 批量上报成功
     * - [EventResult.UploadFailure] 批量上报失败
     * - [EventResult.Empty] 如果没有事件被处理
     */
    suspend fun uploadBatch(events: List<Event>): EventResult
}
