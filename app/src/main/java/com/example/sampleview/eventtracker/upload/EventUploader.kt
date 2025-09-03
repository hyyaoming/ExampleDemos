package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * 事件上报器接口。
 *
 * 定义事件的单条和批量上报行为。具体实现可以是：
 * - 网络请求上传到服务器
 * - 写入本地日志文件或数据库
 * - 自定义处理逻辑
 */
interface EventUploader {
    /**
     * 上报单个事件。
     *
     * @param event 待上报的事件对象
     * @return [EventUploadResult] 表示事件上报的结果：
     *         - [EventUploadResult.Success] 上传成功
     *         - [EventUploadResult.Failure] 上传失败，包含异常信息
     *         - [EventUploadResult.Skipped] 事件被策略跳过
     *         - [EventUploadResult.Empty] 无事件处理（仅批量队列可能出现）
     */
    suspend fun upload(event: Event): EventUploadResult

    /**
     * 批量上报事件。
     *
     * @param events 待上报的事件列表
     * @return [EventUploadResult] 表示批量上传的结果：
     *         - [EventUploadResult.Success] 上传成功，包含事件列表
     *         - [EventUploadResult.Failure] 上传失败，包含异常和事件列表
     *         - [EventUploadResult.Empty] 无事件被处理
     */
    suspend fun uploadBatch(events: List<Event>): EventUploadResult
}
