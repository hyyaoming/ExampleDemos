package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
import com.example.sampleview.eventtracker.queue.EventQueue
import com.example.sampleview.eventtracker.store.PersistentEventStore
import com.example.sampleview.eventtracker.upload.EventUploader
import com.example.sampleview.eventtracker.upload.RetryingUploader


/**
 * ImmediateUploadStrategy 是即时上传策略实现。
 *
 * 特点：
 * 1. 每个事件在处理时立即调用 [EventUploader] 上传。
 * 2. 不依赖队列或批量上传。
 * 3. 支持可选持久化存储，但通常无需恢复队列。
 *
 * @property uploaderConfig 上传器及相关配置
 * @property store 可选持久化存储（通常无需使用）
 * @property queue 可选内存队列（即时上传通常不使用）
 * @property eventUploader 实际执行上传的 [EventUploader]，默认为带重试机制的 [RetryingUploader]
 */
class ImmediateUploadStrategy(
    override val uploaderConfig: EventTrackerConfig.UploaderConfig,
    override val store: PersistentEventStore? = null,
    override val queue: EventQueue? = null,
    override val eventUploader: EventUploader = RetryingUploader(uploaderConfig),
) : EventUploadStrategy {

    /**
     * 处理单个事件并立即上传。
     *
     * @param event 待上传事件
     * @return [EventUploadResult.Success] 上传成功
     *         [EventUploadResult.Failure] 上传失败或异常
     */
    override suspend fun handle(event: Event): EventUploadResult {
        return runCatching {
            val result = eventUploader.upload(event)
            result as? EventUploadResult.Success
                ?: EventUploadResult.Failure(listOf(event), Throwable("Immediate upload failed"))
        }.getOrElse { t ->
            EventUploadResult.Failure(listOf(event), t)
        }
    }

    /**
     * 立即上传策略不支持批量刷新，因此返回 [EventUploadResult.Empty]。
     *
     * @return [EventUploadResult.Empty]
     */
    override suspend fun flush(): EventUploadResult = EventUploadResult.Empty

    /**
     * 立即上传策略无需从持久化存储恢复事件，因此该方法为空实现。
     */
    override suspend fun restore() {}
}
