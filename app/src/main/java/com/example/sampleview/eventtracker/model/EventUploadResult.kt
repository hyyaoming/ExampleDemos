package com.example.sampleview.eventtracker.model

/**
 * 封装事件上报结果。
 */
sealed class EventUploadResult {

    /**
     * 事件已加入队列，等待批量上传。
     */
    object Enqueued : EventUploadResult()

    /**
     * 事件上报成功。
     */
    object Success : EventUploadResult()

    /**
     * 事件上报失败。
     *
     * @property error 上报失败的异常信息
     */
    data class Failure(val error: Throwable) : EventUploadResult()
}
