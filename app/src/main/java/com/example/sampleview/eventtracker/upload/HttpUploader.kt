package com.example.sampleview.eventtracker.upload

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * 基于 HTTP 的事件上报实现。
 *
 * 模拟将事件通过网络发送到服务器。当前示例中使用 [println] 打印事件信息，
 * 在真实应用中可替换为实际的 HTTP 请求逻辑。
 */
class HttpUploader : EventUploader {

    /**
     * 上报单个事件。
     *
     * @param event 待上报的事件
     * @return [EventUploadResult.Success] 表示上报成功
     */
    override suspend fun upload(event: Event): EventUploadResult {
        println("Uploading event '${event.eventId}' to $ with props=${event.properties}")
        return EventUploadResult.Success
    }

    /**
     * 批量上报事件。
     *
     * @param events 待上报的事件列表
     * @return [EventUploadResult.Success] 表示批量上报成功
     */
    override suspend fun uploadBatch(events: List<Event>): EventUploadResult {
        println("Uploading batch of ${events.size} events")
        events.forEach { e ->
            println(" - '${e.eventId}' with props=${e.properties}")
        }
        return EventUploadResult.Success
    }
}
