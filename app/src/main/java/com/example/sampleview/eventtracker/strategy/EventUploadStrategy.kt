package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.upload.EventUploader

/**
 * 事件上报策略接口。
 *
 * 定义事件在不同上传模式下的处理方式，例如立即上报或批量上报。
 * 可以通过自定义实现改变事件的上报逻辑。
 */
interface EventUploadStrategy {

    /**
     * 处理指定事件的上报逻辑。
     *
     * @param event 待上报的事件
     * @param queue 事件队列，允许在批量模式下暂存事件
     * @param uploader 事件上报器，用于执行实际上传
     * @return [EventUploadResult] 表示事件上报的结果
     */
    suspend fun handle(
        event: Event,
        queue: EventQueue,
        uploader: EventUploader,
    ): EventUploadResult
}
