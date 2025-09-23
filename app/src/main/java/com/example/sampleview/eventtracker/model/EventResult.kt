package com.example.sampleview.eventtracker.model

/**
 * EventResult 表示事件在事件管道或上传策略中的处理结果。
 *
 * ### 使用场景
 * - 事件在经过拦截器链、插件链和上传策略处理后，会返回对应的 [EventResult]。
 * - 不同类型表示事件处理的不同状态，包括成功、失败、入队或未处理。
 */
sealed interface EventResult {

    /**
     * 成功上传事件。
     *
     * 表示事件已成功上报到服务器或目标端点。
     *
     * @property events 已成功处理的事件列表
     */
    data class UploadSuccess(val events: List<Event>) : EventResult {
        override fun toString(): String {
            return "上传成功(events=[${events.joinToString { it.toString() }}])"
        }
    }

    /**
     * 上传失败的事件及异常信息。
     *
     * 表示事件在上传过程中发生错误，未能成功上报。
     * 可用于失败重试或日志分析。
     *
     * @property events 处理失败的事件列表
     * @property throwable 导致上传失败的异常
     */
    data class UploadFailure(val events: List<Event>, val throwable: Throwable) : EventResult {
        override fun toString(): String {
            val eventDetails = events.joinToString(separator = ",\n") { it.toString() }
            return "上传失败(events=[\n$eventDetails\n], 异常=${throwable::class.simpleName}: ${throwable.message})"
        }
    }

    /**
     * 网络不可用导致未尝试上传的事件。
     *
     * - 事件因当前网络不可用而未触发上传
     * - 可用于策略层判断是否需要入队或延迟上传
     *
     * @property events 因网络不可用而未上传的事件列表
     */
    data class NetworkUnavailable(val events: List<Event>) : EventResult {
        override fun toString(): String {
            return "网络不可用(events=[${events.joinToString { it.toString() }}])"
        }
    }

    /**
     * 事件已入队等待批量上传，但尚未真正上传。
     *
     * 适用于批量上传策略：
     * - 事件先进入内存队列或持久化队列
     * - 可用于监控队列积压、批量触发上传等
     *
     * @property events 已入队的事件列表
     */
    data class Queued(val events: List<Event>) : EventResult {
        override fun toString(): String {
            return "已入队等待上传(events=[${events.joinToString { it.toString() }}])"
        }
    }

    /**
     * 没有事件被处理。
     *
     * 表示当前策略未处理任何事件：
     * - 队列未满，批量上传尚未触发
     * - 或立即上传策略条件未满足
     */
    object Empty : EventResult {
        override fun toString(): String {
            return "无事件处理"
        }
    }
}
