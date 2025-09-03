package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * HttpUploader 是一个默认的事件上报器实现。
 *
 * - 用于上报单个事件或批量事件
 */
class HttpUploader : EventUploader {

    /**
     * 上报单个事件。
     *
     * 当前实现直接返回 [EventUploadResult.Success]，包含上传的事件。
     *
     * @param event 待上报的事件对象
     * @return [EventUploadResult.Success] 包含已上报的事件列表
     */
    override suspend fun upload(event: Event): EventUploadResult {
        return EventUploadResult.Success(events = listOf(event))
    }

    /**
     * 批量上报事件。
     *
     * 当前实现直接返回 [EventUploadResult.Success]，包含上传的事件列表。
     *
     * @param originalEvents 待上报的事件列表
     * @return [EventUploadResult.Success] 包含已上报的事件列表
     */
    override suspend fun uploadBatch(originalEvents: List<Event>): EventUploadResult {
        return EventUploadResult.Success(events = originalEvents)
    }
}
