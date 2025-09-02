package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * 事件上报器接口。
 *
 * 定义事件的单条和批量上报行为。具体实现可以使用网络请求、日志存储或其他自定义方式。
 */
interface EventUploader {

    /**
     * 上报单个事件。
     *
     * @param event 待上报的事件
     * @return [EventUploadResult] 表示事件上报的结果
     */
    suspend fun upload(event: Event): EventUploadResult

    /**
     * 批量上报事件。
     *
     * @param events 待上报的事件列表
     * @return [EventUploadResult] 表示事件上报的结果
     */
    suspend fun uploadBatch(events: List<Event>): EventUploadResult
}
