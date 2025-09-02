package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.model.UploadMode
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.upload.EventUploader

/**
 * 默认事件上报策略。
 *
 * 根据 [Event.uploadMode] 决定事件的上报方式：
 * - [UploadMode.IMMEDIATE]：立即通过 [EventUploader] 上报事件。
 * - [UploadMode.BATCH]：将事件加入队列 [EventQueue]，等待批量上报。
 */
class DefaultUploadStrategy : EventUploadStrategy {

    /**
     * 处理事件的上报逻辑。
     *
     * @param event 待上报事件
     * @param queue 事件队列，用于批量上报模式
     * @param uploader 上报器，用于立即上报模式
     * @return 上报结果 [EventUploadResult]
     */
    override suspend fun handle(
        event: Event,
        queue: EventQueue,
        uploader: EventUploader
    ): EventUploadResult {
        return when (event.uploadMode) {
            UploadMode.IMMEDIATE -> uploader.upload(event)
            UploadMode.BATCH -> {
                queue.enqueue(event)
                EventUploadResult.Enqueued
            }
        }
    }
}
